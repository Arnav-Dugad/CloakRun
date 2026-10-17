# Changelog

All notable changes to CloakRun are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

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
