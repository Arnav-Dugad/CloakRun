# Changelog

All notable changes to CloakRun are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Play with friends. Host a private lobby on your play area, or join with a six-character code, a QR code or an
  invite link (`arnav-dugad.github.io/join`). Links open the app directly when it is installed.
- Lobby:
  - players orbit the split-flap join code, with live presence
  - the host sets seekers, head start, hunt length and lobby size
  - the host shuffles roles fairly, or taps a player to switch their role
  - the host can remove a player for good
  - players mark themselves ready
- Briefing: everyone sees their role revealed with the safety rules and confirms before play. Live matches (head
  start and hunt) arrive in the next release.
- Onboarding: age gate (13 and over, with a supervision note for under 18s), stay-safe rules, a plain "who sees
  what" consent screen, and a generated name and avatar.
- Profile: edit your name and avatar, link a Google account to keep your profile across phones, turn diagnostics
  on or off, and delete your account and data.
- Accounts are anonymous by default, so no sign-up is needed.
- Security Rules for Firestore and the Realtime Database, tested against the emulator (38 tests) and deployed.

### Changed

- In-app updates install without the system prompt from this version on (Android 12 and newer).

## [0.5.0] - 2026-10-17

### Added

- Park snap: in the play-area designer, choose Park and tap a park, campus green or garden. The area snaps to its
  real edge, stops at main roads, rail and water, and stays editable by dragging corners.
- Safety cutouts. Main roads, railways, water, buildings and restricted land (industrial, military, quarries, rail
  yards, school grounds) inside the area become hatched no-go zones:
  - a summary shows what was excluded and how much of the area it covers
  - an area that is more than 40 percent off limits cannot be saved
  - "Check safety" frames the whole area first, so nothing is missed
- Route plausibility (anti-cheat foundation): an on-device integrity score that flags mock locations, teleports,
  impossible speeds, long stretches away from streets, and straight runs through buildings. Flagged, never banned.
- Simulation Mode shows the live integrity score and adds a Spoof jump to demonstrate detection.

### Changed

- The designer's tool panel collapses to a one-line readout (automatically after a park snap), so the map stays
  visible.

## [0.4.0] - 2026-10-17

### Added

- In-app updates (sideload builds):
  - CloakRun checks GitHub Releases at launch and downloads new versions automatically on Wi-Fi, with a progress bar
    on Home and a progress ring in the update sheet.
  - Every download is verified against the published SHA-256 checksum, GitHub's own digest and the Arnav Games
    signing certificate before it is installed.
  - The sheet shows what is new, the size and each stage (downloading, verifying, installing), and walks you
    through allowing installs the first time.
  - After the first in-app update, Android 12 and newer can update CloakRun without asking again.
- About screen: an Updates card with the current version, Check now, and the auto-download switch.

### Fixed

- Release notes on GitHub now include the changelog section; v0.1.0 to v0.3.0 were corrected.

## [0.3.0] - 2026-10-17

### Added

- Map (Phase 2). Custom CloakRun Night and Day styles on OpenFreeMap vector tiles:
  - near-black land, luminous violet and cyan roads with glow, softly lit parks and water
  - emphasised footpaths
  - 3D buildings that rise with zoom and brighten with height
  The style switches automatically at local sunset and sunrise, with a crossfade between styles.
- Cinematic fly-in to your position: the camera swoops down and tilts as the buildings rise.
- Play-area designer:
  - draw a circle (tap to place it, slider to size it) or a polygon (tap to add corners, drag to move them)
  - a live area readout in square metres, hectares or square kilometres, with validation against size limits and
    crossing edges
  - an animated glowing energy-line border, with the outside world dimmed
  - areas are saved on the device only
- Location service:
  - one shared, adaptive location stream that samples fast near opponents, slow when idle, and frugally on low
    battery or battery saver
  - mock-location flagging
  - a match foreground service with a persistent notification and a Stop action
  - an app-wide "Sharing your fuzzy zone" indicator
- Local geofencing for play-area enter, exit and near-edge, with GPS-jitter hysteresis.
- Home screen and Navigation 3 app shell with spring transitions and a predictive-back preview.
- Simulation Mode (debug builds only):
  - bots that walk the real street and path network of the visible map, drawn as seeker heading cones and hider
    glass beads with name tags
  - "Walk me" to simulate your own movement
  - 1x, 4x and 10x speed
  - a toggle for the match service
  - a debug HUD with frame rate, fix accuracy, source and mock flag
- Pure Kotlin game engine foundations: geodesy, play-area geometry, sunrise and sunset, a road graph, bot walkers and a
  fast-forwardable game clock, all unit tested.

### Fixed

- The wordmark is now legible on light themes (single-colour variant).

## [0.2.0] - 2026-10-17

### Added

- Design system (Phase 1).
  - Frost Glass material: backdrop blur (Haze 2), an 8 to 16 percent tint, a 1dp gradient rim traced along the
    shape, film grain, a tinted soft shadow and a specular sweep that follows device tilt. Glass quality adapts to
    the device: full blur, reduced blur, or a dense frosted tint below Android 12, on low-RAM phones and in
    Performance mode.
  - Continuous-corner `SquircleShape` (superellipse, n = 5) that eases into a true capsule for pills.
  - Components: buttons (primary, glass, ghost, danger) with squash and stretch and a highlight under the finger;
    hold-to-confirm for destructive actions; chips; a liquid toggle; segmented tabs with a sliding glass indicator;
    a slider; a glass text field; a rolling number ticker; split-flap join-code text; an in-match status pill; a
    radar loader; empty and error states; a detented glass bottom sheet whose corners, blur and scrim follow the
    drag; and procedural avatars.
  - Haptic vocabulary (tick, confirm, ping, near, caught, power-up, countdown, heartbeat, error) built on
    `VibrationEffect.Composition` primitives with fallbacks for older devices.
- Custom icon set: 52 icons on a 24dp grid with a 2dp round stroke, including every icon in the brief. Directional
  icons mirror in right-to-left layouts.
- Design Gallery (debug builds only, "CloakRun Gallery" in the launcher) showing every token, component, icon,
  avatar and haptic cue, with theme, reduced-motion and Performance-mode switches.
- Screenshot tests (Roborazzi) for every gallery section in the Night and Daylight themes.

### Changed

- The About screen now uses real glass cards over the radar backdrop.

## [0.1.0] - 2026-10-17

### Added

- Foundation (Phase 0). A multi-module Gradle build with convention plugins (AGP 9.4, Kotlin 2.4, Compose, Hilt),
  `sideload` and `play` product flavours, and release signing from CI secrets.
- Brand assets generated from `branding/`. An adaptive launcher icon with a themed (monochrome) layer, an animated
  Android 12+ splash where the emblem draws on, the cloak fills and the radar ring expands, a static splash for older
  Android versions, in-app emblem and wordmark vectors, a README banner and a GitHub social preview.
- Frost Glass design tokens: Night, AMOLED and Daylight colour schemes, Sora and Inter variable typography with
  tabular figures, spacing, continuous-corner radii, spring motion tokens and live detection of the system
  "remove animations" setting.
- Palette accessibility tests: WCAG contrast on every theme, plus team and status colours checked under simulated
  protanopia, deuteranopia and tritanopia.
- About screen with the privacy promise, build information, open-source notices and "Made by Arnav Games".
- Firebase project layout (Firestore, Realtime Database, emulators) with deny-by-default Security Rules and
  emulator-backed rules tests.
- CI (formatting, detekt, strict lint, unit tests, rules tests, debug build) and a tag-driven release workflow that
  publishes a signed APK, an AAB and SHA-256 checksums.
- Documentation: architecture, decisions, Firebase setup, Spark quota budget, releasing, test plan and the ideas
  backlog.
