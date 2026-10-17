# Quota budget (Firebase Spark plan)

CloakRun must run entirely on the free Spark plan. This document fixes the numbers the client is built around. Limits
were verified on 2026-10-17 against firebase.google.com/pricing, /docs/firestore/quotas and
/docs/database/usage/limits.

## Spark limits we design against

| Product | Limit | Consequence for CloakRun |
| --- | --- | --- |
| Firestore | 50,000 reads, 20,000 writes, 20,000 deletes per day; 1 GiB stored; 10 GiB/month egress | Low-frequency state only. Reads are the binding constraint. |
| Firestore TTL | Not free (needs billing) | Cleanup is app-driven (see below). |
| Realtime Database | **100 simultaneous connections**, 1 GB stored, 10 GB/month download, one database | High-frequency fuzzed zones only. Connections are the binding constraint. |
| Remote Config | 100,000 fetches per day | Fetch at most every 12 hours, plus once at match start. |
| Authentication | 3,000 daily active users (Tier 1 providers); 100 anonymous sign-ups per hour per IP | Comfortable for private lobbies. |
| Play Integrity (App Check) | 10,000 requests per day | Tokens cached for their full lifetime. App Check is in monitor mode. |
| Cloud Functions, Cloud Storage | Not available | No server code, no uploads. |

## Reference match: 8 players, 30 minutes

Assumptions: 5-minute lobby, 60-second head start, 30-minute hunt including the final circle, then results.
Every client keeps three Firestore listeners: the match document, its players subcollection and its events
subcollection. A Firestore listener is billed one read per changed document delivered to it.

### Firestore writes

| Activity | Writes |
| --- | --- |
| Create match and join code | 2 |
| 8 players join | 8 |
| Ready toggles (about 2 each) | 16 |
| Host edits play area, mode and rules | 10 |
| Role assignment (batched player updates) | 8 |
| Phase transitions (lobby, head start, hunt, final circle, results) | 4 |
| Game events during the hunt: tag claims and verdicts (20), power uses (48), cache spawns and pickups (24) | 92 |
| Player status changes (caught, spectral) | 6 |
| Results summary and per-player stats | 9 |
| **Total** | **155** |

### Firestore reads

| Activity | Reads |
| --- | --- |
| Initial listener snapshots (match 1 + players 8, per client) | 72 |
| Player document changes (38 writes x 8 listeners) | 304 |
| Match document changes (about 15 x 8) | 120 |
| Events (92 x 8) | 736 |
| Results screen | 8 |
| Reconnect and resume overhead (20 percent) | 248 |
| Security Rules lookups (`get` and `exists` inside rules are billed as reads; about 20 per player per match) | 160 |
| **Total** | **about 1,650** |

### Firestore deletes (end-of-match cleanup)

Events (92), players (8), match (1), join code (1): **102**.

### Daily capacity

| Quota | Per match | Daily limit | Matches per day |
| --- | --- | --- | --- |
| Reads | 1,650 | 50,000 | **30** (binding) |
| Writes | 155 | 20,000 | 129 |
| Deletes | 102 | 20,000 | 196 |

### Realtime Database

Each player publishes a fuzzed zone (cell-snapped centre, radius, status, team, server timestamp). The payload is
about 120 bytes. With protocol framing we budget 200 bytes per delivered update.

- Publish interval: adaptive between 5 and 30 seconds. We budget an average of 10 seconds, so each player sends
  1,800 / 10 = 180 updates per match.
- Fan-out: each update reaches 7 other players. 8 x 180 x 7 x 200 B = **2.0 MB of download per match**.
- Presence, joins and initial snapshots add about 0.5 MB. We budget **2.5 MB per match**.
- 10 GB per month / 2.5 MB = **about 4,000 matches per month** (about 130 per day).
- Storage is negligible: nodes are removed on match end.
- **Connections are the real limit.** 100 simultaneous connections allows 12 concurrent 8-player matches (96
  connections). Clients connect to RTDB only while in a lobby or match, and call `goOffline()` everywhere else.

### Verdict

A single Spark project comfortably supports **about 30 full 8-player matches per day**, with Firestore reads as the
binding constraint, and **at most 12 matches at the same moment**, with RTDB connections as the binding constraint.
That fits private play among friends. Public matchmaking would not fit, and it is out of scope for v1 by design.

## Guards built into the client

1. **Kill switches (Remote Config).** `rtdb_enabled` and `matches_enabled`. When off, the app explains that
   CloakRun is at capacity and offers a retry, instead of failing mysteriously.
2. **Client-side throttling.** RTDB writes go through a token bucket with a minimum interval
   (`rtdb_min_write_interval_ms`, default 5000) that only grows under quota pressure. Security Rules reject writes
   less than 4 seconds after the previous one, using server timestamps, so a modified client cannot flood.
3. **Auto-degrade.** If a write is rejected or the debug quota counter crosses 80 percent of its per-match budget,
   the interval doubles, up to 30 seconds.
4. **Listener hygiene.** Listeners are scoped to the active match and removed on exit. The events listener uses a
   `createdAt` cursor so a reconnect does not re-read history.
5. **Size limits.** Rules cap document and node sizes, such as name length, event payloads and the polygon vertex
   count.
6. **App-driven cleanup.** The host deletes the match tree at the end. Matches carry `expireAt`. Any member may
   delete an expired match, and the app sweeps expired matches it can see on launch. RTDB nodes use `onDisconnect`
   removal plus a host sweep.
7. **Debug overlay.** Debug builds show live counters of reads, writes, RTDB bytes and listener count per match, so
   the budget can be checked in Simulation Mode.

## When the budget changes

Update this file whenever a new feature adds listeners, events or RTDB traffic, and recompute the matches-per-day
figures. A change that drops capacity below 20 matches per day needs an explicit decision in `DECISIONS.md`.
