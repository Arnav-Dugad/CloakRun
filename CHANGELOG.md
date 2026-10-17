# Changelog

All notable changes to CloakRun are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

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
