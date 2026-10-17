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

## D-020 MapLibre Native through AndroidView, OpenGL ES backend (2026-10-17)

The MapLibre Compose wrapper (`org.maplibre.compose` 0.20) is still pre-1.0. Its public API is being reshaped and it
no longer uses the Android SDK, so it is not used. `CloakMap` wraps `org.maplibre.gl:android-sdk-opengl` 13.7.1 in
an `AndroidView` and forwards lifecycle. The OpenGL ES artifact is chosen over the default Vulkan one for the widest
device compatibility down to Android 8. When glass blur is active the map renders into a TextureView, because a
SurfaceView cannot be captured by RenderEffect, so glass panels blur the live map. Other tiers keep the cheaper
SurfaceView.

## D-021 Game overlays are Compose, synced to map frames (2026-10-17)

The player dot, play-area energy line, bots, and later the fuzzy zones and radar are drawn in Compose above the map.
They are not MapLibre style layers, because they need springs, AGSL shaders and per-frame animation that style
layers cannot express. `CloakMapState.frame` increments on every rendered map frame. Overlays read it in draw or
layout scope, so they follow pans and zooms without recomposing. Static geography stays in the style.

## D-022 Debug tooling enters through build-type source sets (2026-10-17)

`app/src/debug` supplies the Simulation Mode overlay, and `app/src/release` supplies an empty list. Release code never
references `feature:devtools`. The only hook in shipped code is the inert `LocationRepository.sourceOverride`, which
release builds never set.

## D-023 Location architecture (2026-10-17)

A single `LocationRepository` owns every location read. Callers declare a `TrackingMode`. The repository applies
`SamplingPolicy` to battery, power-saver and opponent distance, and shares one fused request between all
collectors. Location is read only while scouting with the screen visible, in a lobby, or during a match. The match
foreground service (type `location`) is always started from the foreground, so `ACCESS_BACKGROUND_LOCATION` is never
requested. An app-wide pill shows whenever the service runs, and screens reserve room for it through
`LocalTopChromeInset`.

## D-024 Local geofencing (2026-10-17)

Zone enter, exit and near-edge are computed on the device by `ZoneWatcher` on every fix, with accuracy-scaled
hysteresis. Platform geofences are not used: they support circles only, need background location for reliable
delivery, and can fire minutes late.

## D-025 Play-area limits (2026-10-17)

The default limits are 2 ha to 3 km² and 3 to 24 polygon corners. The circle radius therefore runs from 80 m to
977 m. Self-intersecting polygons are rejected. The limits live in `PlayAreaRules` and will be tunable from Remote
Config.

## D-026 Wordmark on light themes (2026-10-17)

The lockup's luminous wordmark is designed for dark grounds and nearly vanishes on Daylight. Light themes render it
as the single-colour ink variant (a tint, not a redraw). This was caught by the Home screenshot test.

## D-027 In-app updater (2026-10-17)

The sideload flavour updates itself from GitHub Releases. The play flavour has no updater and no
`REQUEST_INSTALL_PACKAGES`.

- **Check:** at launch, at most every 6 hours, through the unauthenticated GitHub API (60 requests per hour per IP is
  plenty). Drafts, pre-releases and releases without `CloakRun-vX.Y.Z.apk` are ignored.
- **Download:** `DownloadManager` into app-specific storage, so it resumes across process death and needs no storage
  permission. Progress is polled every 250 ms. When "Download updates on Wi-Fi automatically" is on (the default),
  downloads start by themselves on unmetered networks.
- **Verify:**
  - The SHA-256 must match the published `SHA256SUMS.txt` and GitHub's own asset digest, whichever are available,
    and at least one must be.
  - The APK must have the same package name as the installed app and be signed only by certificates the installed
    app trusts.
  - Failures delete the file and never install.
- **Install:** a `PackageInstaller` session rather than handing a FileProvider URI to the installer activity, which is
  what the brief described.
  - Sessions report staging progress and use the system confirmation.
  - Once CloakRun is the installer of record, Android 12+ can skip the prompt (`USER_ACTION_NOT_REQUIRED`). This was
    verified on the emulator: after one in-app update, the installer of record is `com.arnavgames.cloakrun`.
  - Skipping the prompt also needs the normal permission `UPDATE_PACKAGES_WITHOUT_USER_ACTION` in the running
    version. The v0.4.0 to v0.5.0 test still prompted because it was missing. It was added in v0.6.0, so updates from
    v0.6.0 onwards install without a prompt.
- **Debug builds** never update from GitHub: they use the `.debug` package and the debug key.

## D-028 Release notes extraction (2026-10-17)

The release workflow extracted the changelog section with an awk regular expression whose escapes were lost, so
v0.1.0 to v0.3.0 shipped notes without their changelog. It now matches the heading as a literal string, and the three
existing releases were re-published with full notes. The updater shows these notes in its sheet.

## D-029 Safety cutouts and what counts as restricted land (2026-10-17)

The safety pass treats five kinds of place inside a play area as no-go zones. All of them come from the rendered
OpenMapTiles features in the viewport, simplified to 1.5 m:

- **Water:** the `water` layer.
- **Major roads:** motorway, trunk and primary roads, buffered by 18, 14 and 10 m.
- **Rail:** rail and transit lines, buffered by 8 m.
- **Buildings:** never part of the game.
- **Restricted land:** the OpenMapTiles schema has no `access` tags for areas, so private land is approximated by
  land-use classes that are rarely public: military, industrial, quarry, railway, garages, dam, school and
  kindergarten. This is deliberately conservative. A playground or park is never excluded, and a school ground is
  excluded even if it is open after hours.

No-go zones are hatched on the map and summarised with a coverage percentage. An area that is more than 40 percent
no-go cannot be saved. The query needs zoom 15 or closer (buildings render from zoom 13 and footpaths from 14), so
"Check safety" frames the area top-down at zoom 15 or closer before querying. Hazards are recomputed on each device
and are not uploaded with the area.

## D-030 Park snap by rasterisation (2026-10-17)

Vector tiles clip features at tile edges, so a park arrives as several pieces. Rather than a fragile polygon union,
`ParkSnap` works on a local grid (cell size at least 3 m, at most 420 cells per side):

1. Rasterise the pieces.
2. Block water, major roads and rail first, so a road through a park splits it.
3. Flood-fill from the tap.
4. Fill holes (ponds and buildings become no-go zones, not holes).
5. Close diagonal saddles.
6. Trace the outer boundary along cell edges.
7. Simplify with Douglas-Peucker, raising the tolerance until the outline has at most 24 corners and no crossing
   edges.

This is deterministic and unit tested with tile-split, road-split, pond, serrated and too-small cases.

## D-031 Route plausibility (2026-10-17)

An on-device check of a trail flags these, and only fixes with accuracy of 25 m or better are judged:

| Flag | Rule | Penalty |
| --- | --- | --- |
| Mock location | Any fix from a mock provider | 60 |
| Teleport | More than 120 m beyond GPS error within 5 s | 35 |
| Impossible speed | Over 14 m/s, or over 8.5 m/s for 3 segments | 20 |
| Through buildings | A straight run of 25 m or more through a footprint | 15 |
| Off network | More than 45 m plus accuracy from any street or path for 60 s | 10 |

The score starts at 100. 85 or more is Clean, 50 or more is Suspicious, and anything lower is Likely spoofed. In
line with the brief, results are flagged to the host and never auto-banned. It complements the Bluetooth proximity
proof planned for tags in Phase 4.

## D-032 Lobby data model and who may write what (2026-10-17)

There is no server, so the Security Rules carry the authority. The model is shaped so each rule can be checked
from a single write.

**Joining and leaving** are transactions. Each one moves `matches/{id}.playerCount` together with creating or
deleting the player's own document. The rules check both sides with `getAfter`, so the cap (2 to 8 players) holds
without a server.

**Players** may change their own name, avatar and readiness in the lobby, and confirm the briefing during the
briefing. Writes are rate limited to one per 400 ms.

**The host** may:
- assign roles, in the lobby only
- change settings, in the lobby only
- move the phase along the legal graph: lobby, briefing, headstart, hunt, results, then lobby again, with `ended`
  reachable from any phase
- kick one player per write

A kick appends to `kicked`, decrements the count and deletes the player document in the same transaction. Kicked
players can never rejoin that lobby.

**Expiry.** Firestore TTL needs billing, so expiry is `createdAt + 6 h`, checked by the rules. Expired lobbies
cannot be joined, and anyone signed in may delete them.

**Visibility.** A signed-in player may `get` a match when they know its random id, which means they were given the
code. Listing matches or codes is never allowed. Only members can read the player list.

**Join codes** live in `joinCodes/{code}` and are created in the same batch as the match. A taken code fails that
batch and the client retries with a new code.

## D-033 Realtime Database membership mirror (2026-10-17)

RTDB rules cannot read Firestore, so `/lobbies/{match}` mirrors membership:
- **host:** written once by the first claimant, which is the creating client
- **members:** each player adds or removes only themselves, and only while the lobby has a host and they are not
  kicked
- **kicked:** written only by the host

Presence (`/presence`) and fuzzed zones (`/public`) are readable by members only. Zones are validated to have a
radius of at least 25 m and no extra fields, and one update per 2 seconds. Presence uses `onDisconnect`, so a phone
that drops shows as offline.

## D-034 Invite links on the GitHub Pages user site (2026-10-17)

Invites are `https://arnav-dugad.github.io/join/?code=XXXXXX`, served from the `Arnav-Dugad/Arnav-Dugad.github.io`
repository. The QR code and the share text use the same URL:
- With CloakRun installed, the verified App Link opens the app straight into joining.
- Without it, the page shows the code and a download button.

`/.well-known/assetlinks.json` lists the release package with the release key, and the debug package with this
machine's debug key. Verified on the API 36 emulator: `arnav-dugad.github.io: verified`.

That repository is the source of truth for the website, including the `/cloakrun/` landing page. The root redirects
there until there is more than one game.

## D-035 Onboarding, age gate and diagnostics consent (2026-10-17)

**Age gate.** It is neutral: three equal buttons, none preselected. Only the band (under 13, 13 to 17, 18 or older)
is stored, on the phone. Under 13 is blocked. The band survives account deletion and the back button, so the gate
cannot simply be retried. 13 to 17 sees a supervision note.

**Consent screen.** It says who sees what in plain language. Crash reports, Analytics and Performance are off in the
manifest and stay off unless the player opts in. Debug builds never report. Analytics never collects the
advertising ID and has ad personalisation signals off.

**Names.** A friendly generated name and avatar are prefilled, so nobody has to type before playing. Names are 2 to
20 letters (any script), digits, spaces, dots, dashes or underscores.

## D-036 The briefing ends Phase 3 (2026-10-17)

Starting a match moves the lobby to `briefing`. Each player sees their role revealed and the safety rules, and
confirms. The host sees progress and can go back to the lobby for a rematch.

The head start and hunt arrive with the match engine in Phase 4. Until then the briefing says so plainly, rather
than offering a start button that does nothing.
