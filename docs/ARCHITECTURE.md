# CloakRun architecture

This document describes the target architecture. Modules are created when the phase that needs them starts, so
the repository never carries empty shells. The phase in which each module lands is noted below.

## Principles

1. **The hider's exact position never leaves their phone.** Every layer is designed around this. The cloud only
   ever sees a fuzzed zone (cell-snapped centre plus a shrinking radius) and a status.
2. **Pure game engine.** All rules (phases, modes, powers, zone shrink, fuzzing, tag validation, role assignment)
   live in `core:game`, a JVM-only Kotlin module with no Android dependency, driven by an injected clock and seeded
   randomness so every rule is deterministically unit-testable.
3. **Unidirectional data flow.** Each screen has one `StateFlow<UiState>`, receives `Intent`s, and emits one-shot
   `Effect`s through a `Channel`. ViewModels never expose mutable state.
4. **Quota-aware by construction.** Firestore holds low-frequency state only. Realtime Database holds presence and
   fuzzed zones only, behind a client-side throttle and a Remote Config kill switch. See `QUOTA_BUDGET.md`.
5. **Host as referee.** There are no servers (Spark plan). The host device validates claims; Security Rules enforce
   everything they can (membership, ownership, legal transitions, timestamps, sizes, write rates).

## Module graph

```
app ─┬─ feature:onboarding ─┐
     ├─ feature:home        │
     ├─ feature:lobby       ├── core:designsystem ── core:icons
     ├─ feature:match       │        │
     ├─ feature:profile     │        └── (Haze, AGSL shaders, haptics, sound)
     ├─ feature:settings    │
     ├─ feature:update  (sideload flavour only)
     ├─ feature:devtools (debugImplementation only: Design Gallery, Simulation Mode, debug overlay)
     │
     ├─ core:map ──────── MapLibre Native, styles, overlays (fuzzy blob, radar, fog, trails)
     ├─ core:location ─── foreground service, adaptive sampling, geofences, BLE ephemeral IDs
     ├─ core:data ─────── Firebase (Auth, Firestore, RTDB, Remote Config, App Check), DataStore
     └─ core:game ─────── pure Kotlin rules engine (no Android)

benchmark ── Macrobenchmark + Baseline Profile generator (targets app)
build-logic ── Gradle convention plugins shared by every module
```

| Module | Kind | Lands in |
| --- | --- | --- |
| `build-logic` | Gradle included build | Phase 0 |
| `app` | Android application | Phase 0 |
| `core:designsystem` | Android library + Compose | Phase 0 (tokens), Phase 1 (components) |
| `core:icons` | Android library + Compose | Phase 1 |
| `feature:devtools` | Android library, debug only | Phase 1 |
| `core:map`, `core:location` | Android library | Phase 2 |
| `core:game` | JVM library | Phase 2 (geometry), Phase 4 (rules) |
| `core:data` | Android library | Phase 3 |
| `feature:*` | Android library + Compose | Phases 3 to 5 |
| `feature:update` | Android library, sideload flavour | Phase 7 |
| `benchmark` | `com.android.test` | Phase 6 |

## Product flavours

| Flavour | Distribution | Differences |
| --- | --- | --- |
| `sideload` | GitHub Releases APK | Includes `feature:update` and `REQUEST_INSTALL_PACKAGES`. |
| `play` | Google Play AAB (future) | Omits the updater and the install-packages permission. |

## Data layout (summary)

Firestore (low frequency):

- `matches/{matchId}`: lobby config, phase, host uid, mode, play area, timestamps, `expireAt` for TTL.
- `matches/{matchId}/players/{uid}`: display name, avatar seed, role, team, ready, status.
- `matches/{matchId}/events/{eventId}`: append-only game events (tag claims, power use, verdicts).
- `joinCodes/{code}`: maps a 6-character code to a match id, with `expireAt`.
- `users/{uid}`: profile, avatar seed, stats. No location ever.

Realtime Database (high frequency, small, ephemeral):

- `/presence/{matchId}/{uid}`: online flag with `onDisconnect` cleanup.
- `/public/{matchId}/{uid}`: fuzzed zone centre (cell-snapped), radius, status, team, server timestamp.

On-device only: exact GPS fixes, the trail used for replay, BLE ephemeral ID secrets, the integrity log draft.

## Threading and state

- Coroutines and Flow everywhere. Dispatchers are injected, never hard-coded.
- Location is collected in a foreground service and exposed as a `SharedFlow` to the active match.
- The match engine reduces `(MatchState, GameEvent) -> MatchState` and is time-driven by a ticking `Clock` flow.
