# Decisions log

Each entry records a decision, why it was made, and when it should be revisited. Newest last.
Versions and limits were verified against official sources on the date shown.

## D-001 Repository layout and the brand folder (2026-10-17)

The brand sources live in `branding/` (lowercase, as the spec requires). On the original Windows checkout the folder
was named `Branding/` and was locked by the editor, so git records it as `branding/` directly in the index. Linux CI
and fresh clones see `branding/`. The SVGs themselves are untouched and remain the source of truth.

## D-002 Toolchain versions (2026-10-17)

| Component | Version | Note |
| --- | --- | --- |
| Gradle | 9.8.1 | Wrapper pinned by SHA-256. Runs on JDK 17 to 27. |
| Android Gradle Plugin | 9.4.1 | Needs Gradle 9.6+. Supports API 37. |
| Kotlin | 2.4.21 | AGP 9 built-in Kotlin. `org.jetbrains.kotlin.android` must not be applied. |
| KSP | 2.3.12 | KSP2 only (KSP1 removed). |
| Compose BOM | 2026.09.00 | Compose 1.12.1, Material 3 1.4.0. |
| Hilt | 2.60.1 | Requires AGP 9+, Gradle 9.1+, minSdk 23+. |
| compileSdk / targetSdk | 37 | Android 17 is stable (June 2026). Play requires 36+ since 2026-08-31. |
| minSdk | 26 | As specified. Above the floor of every dependency (Firebase BoM 35 needs 24). |

Kotlin 2.4.21 is officially tested up to AGP 9.3.1. It builds cleanly on 9.4.1. Revisit when Kotlin 2.5 ships.

## D-003 Build JDK is 25 (2026-10-17)

CI and local builds run on JDK 25 (Temurin in CI, the Android Studio JBR locally). detekt 2.0 requires it and Gradle
9.8 supports it. Bytecode targets Java 17.

## D-004 detekt 2.0.0-alpha.6 instead of a stable release (2026-10-17)

The latest stable detekt (1.23.8) predates Kotlin 2.1 and AGP 9 and does not work with built-in Kotlin. The only line
that supports Kotlin 2.4 and AGP 9 is 2.0.0-alpha.6 (plugin id `dev.detekt`). We pin the alpha and the matching
compose-rules 0.6.8. Revisit when 2.0.0 is stable.

## D-005 Formatting with Spotless and ktlint, settings passed explicitly (2026-10-17)

Spotless 8.10.4 runs ktlint 1.8.0 with the compose-rules ktlint ruleset. Spotless did not pick up `.editorconfig` on
Windows paths with spaces, so the same settings are passed with `editorConfigOverride` in the root build file.
`.editorconfig` mirrors them for the IDE.

## D-006 Navigation 3 (2026-10-17)

The spec asks for Navigation Compose with shared-element transitions and predictive back. AndroidX Navigation 3
(1.2.0, stable since 2026-09-23) is the Compose-first navigation library. It models the back stack as plain state,
supports shared elements and predictive back, and suits the cinematic transitions we need. We adopt it from
Phase 1 onwards. `navigation-compose` 2.10.2 remains a fallback if a blocker appears.

## D-007 Firebase is optional at build time (2026-10-17)

`google-services.json` is git-ignored. The Google Services plugin is applied only when the file exists, so forks and
fresh clones build and run (offline) without it. CI writes the file from the `GOOGLE_SERVICES_JSON_B64` secret.
Firebase SDKs arrive in Phase 3. The Firebase Performance Gradle plugin (2.0.2) is not confirmed on AGP 9.4, so we use
the Performance SDK without bytecode instrumentation. That gives app start and screen traces, and network traces are
added manually.

## D-008 Spark plan realities (2026-10-17)

- **Firestore TTL policies are not free.** They need billing. Matches are cleaned up by the host when a match ends,
  and Security Rules let any match member delete an expired match, so stale data cannot accumulate. See
  `QUOTA_BUDGET.md`.
- **Cloud Storage is unavailable on Spark** (since 2026-02-03). Avatars are procedurally generated vectors.
- **Cloud Functions need Blaze.** There is no server code. The host device referees, and Security Rules enforce what
  they can.
- **Dynamic Links shut down on 2025-08-25.** Invites use join codes, QR codes and App Links.
- **Remote Config pricing changed on 2026-09-01.** Spark allows up to 100,000 fetches per day. We fetch at most
  once per 12 hours plus once at match start.
- **App Check with Play Integrity needs `PLAY_RECOGNIZED` by default**, which sideloaded builds never get. App Check
  runs in monitor mode, and enforcement is a Remote Config flag (`appcheck_enforce`). The 10,000 requests per day
  Play Integrity quota is shared, so tokens are cached for their full lifetime.

## D-009 Map tiles from OpenFreeMap (2026-10-17)

OpenFreeMap needs no key and no registration, allows commercial use, and sets no request limits. It serves
OpenMapTiles-schema vector tiles through a TileJSON at `https://tiles.openfreemap.org/planet`, plus glyphs (Noto Sans
Regular, Bold and Italic). Protomaps' hosted API needs a key and a sponsorship for commercial use. Self-hosting a
120 GB PMTiles planet is out of scope for a free project. OpenFreeMap is donation-funded with no SLA, so the style
URL is overridable from Remote Config. Attribution (OpenMapTiles and OpenStreetMap) is always shown. Offline caching
respects their limit of 3,000 tiles or 50 MB per download.

## D-010 Fonts: Sora (display) and Inter (text) (2026-10-17)

Both are SIL OFL 1.1, variable and support tabular figures (`tnum`). Sora is geometric with a premium feel and is
only 111 KB. Inter has an optical-size axis, used at opsz 16 for text and 32 for large figures. The licence texts
ship in `docs/licenses/` and are credited in the About screen.

## D-011 Team colours cyan and amber (2026-10-17)

Blue against orange is the pairing that survives all three dichromacies. The hider team is signal cyan and the seeker
team is amber. `PaletteAccessibilityTest` simulates protanopia, deuteranopia and tritanopia (Machado 2009) and
requires a CIE delta-E of at least 25 between the teams. Team colour is never the only cue, because seekers are cones
and hiders are dots.

## D-012 Primary container for filled controls (2026-10-17)

White text on the brand violet `#7C5CFF` reaches 4.35:1, below WCAG AA. Filled buttons use `primaryContainer`
(`#6448F0`, 5.6:1). The brand violet stays for glows, focus rings and accents. Daylight danger is `#A3122A` so it
stays distinct from success under protanopia.

## D-013 App Links host (2026-10-17, open)

App Links verification is per host, and only `https://<host>/.well-known/assetlinks.json` is consulted. A project page
(`arnav-dugad.github.io/CloakRun`) cannot serve that path. It must come from a user-site repository named
`Arnav-Dugad.github.io` with a `.nojekyll` file. This needs the owner's approval (a second public repository) and is
deferred to Phase 3, when invites ship. Until then, invites use join codes and QR.

## D-014 Versioning (2026-10-17)

`version.properties` holds `VERSION_NAME` and `VERSION_CODE` and is read by Gradle. Only `scripts/release.sh` changes
them. `versionCode` increases by one per release.

## D-015 Gradle JVM networking (2026-10-17)

On the development machine, JVM connections over IPv6 time out while curl succeeds. `org.gradle.jvmargs` sets
`-Djava.net.preferIPv4Stack=true`. For the wrapper download, export
`JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`. This is harmless in CI.

## D-016 Glass via Haze 2 with three quality tiers (2026-10-17)

Haze 2.0.1 (`haze` and `haze-blur`) provides backdrop blur through RenderEffect on API 31+. `GlassSurface` wraps it
and adds what Haze does not: the gradient rim along the exact shape, grain, a tilt specular and a tinted shadow.
`GlassQuality` picks one of three tiers:

- **Full:** API 31+ with a normal memory class.
- **Reduced:** blur at 60 percent radius and no grain, for small memory classes.
- **Tint:** no blur, with a denser frosted tint that keeps text contrast. This tier applies below API 31, on low-RAM
  devices, and in Performance mode.

Device tilt is read once in `CloakTheme` and shared through a composition local, and only in draw scope, so tilt
never triggers recomposition.

## D-017 Continuous corners by superellipse (2026-10-17)

`SquircleShape` builds each corner from the superellipse |x|^n + |y|^n = 1 (n = 5), spread over 1.6 times the
nominal radius. This makes it read at the same visual size as a circular corner but with curvature continuity. When
the corner is clamped (pills), the exponent eases toward 2, so capsules have true semicircular ends. Tests rasterise
the path and probe points inside and outside each corner.

## D-018 Screenshot testing with Roborazzi on Robolectric (2026-10-17)

Roborazzi 1.76 with Robolectric 4.17 native graphics renders every gallery section in the Night and Daylight themes
at 411 x 1500 dp, xxhdpi (tall enough for the longest section). Goldens live in `feature/devtools/src/test/screenshots` and are verified in CI with a
2 percent tolerance for anti-aliasing differences between operating systems. Robolectric on JDK 25 needs
`java.base` internals opened, so the convention plugin adds the `--add-opens` and `--add-exports` flags to every
Android test task. Screenshots render with reduced motion, so infinite animations stop and the clock can go idle.
This also enforces that no component animates forever when motion is reduced.

## D-019 Developer tools are a debug-only module (2026-10-17)

`feature:devtools` holds the Design Gallery now, and Simulation Mode and the debug overlay in Phase 2. The app depends
on it with `debugImplementation`, so its code, strings and launcher entry never reach release builds.
