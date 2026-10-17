# CloakRun: standing context for coding agents

CloakRun is a premium, real-world, location-based hide and seek game for Android, made by Arnav Games.
Package `com.arnavgames.cloakrun`. This file is kept identical to `AGENTS.md`. Edit both together.

Quick orientation:

- Decisions log: `docs/DECISIONS.md`. Architecture: `docs/ARCHITECTURE.md`. Ideas backlog: `docs/IDEAS.md`.
- Firebase manual steps: `docs/FIREBASE_SETUP.md`. Quota budget: `docs/QUOTA_BUDGET.md`.
- Build JDK: Android Studio's bundled JBR (`C:\Program Files\Android\Android Studio\jbr`) or any JDK 21+.
- Verify everything locally with `./gradlew spotlessCheck detekt lint testDebugUnitTest assembleDebug` before committing.
- Rules tests: `cd firebase/tests && npm ci && npm test` (requires the Firebase Emulator Suite and a JDK).

## 0. Ground rules (read first, obey always)

- Verify before you use. Library versions, Firebase free-tier limits, tile-server URLs and API names change. Web-search the current docs for every dependency and quota before relying on it. Never trust your memory for versions.
- No emojis anywhere: not in the UI, strings, commit messages, README, logs or code comments. All iconography is custom (section 7).
- Ask only blocking questions, in one batch. Otherwise decide, write the decision in docs/DECISIONS.md, and keep moving.
- Never print, log or commit secrets (keystores, passwords, tokens, service accounts).
- Work in small verified steps. After each step: build, run tests and lint. Fix failures before moving on. Commit with Conventional Commits.
- Do the real work. No placeholder screens, no fake data left in release builds, no "TODO: implement" in shipped paths.
- On first run, create CLAUDE.md and AGENTS.md (identical content) at the repo root containing sections 0, 7, 8, 11 and 12 of this prompt, so context survives across sessions in both tools.

## 7. Icons and brand (no emojis, no stock icon packs)

- Draw a custom icon set as Kotlin ImageVectors or VectorDrawables on a 24dp grid with a consistent 2dp stroke and rounded caps. Do not use Material Icons. Minimum set: radar, cloak, decoy, sonar, freeze, trap, cache, zone, tag, hider, seeker, ghost, trophy, lobby, invite, qr, sos, battery, signal, compass, settings, profile, replay, shield, eye, eye-off, timer, team. Put them in core:icons and show them all in the Design Gallery.
- Logo: the source of truth is branding/cloakrun-icon.svg (app icon) and branding/cloakrun-logo.svg (lockup). Do not redesign them. From the icon, generate: Android adaptive icon (foreground, background, and monochrome layers for themed icons), an animated Android 12+ splash icon, a README banner, a 1280x640 GitHub social preview, and in-app logo vectors. Footer wording on splash and About: "Made by Arnav Games".

## 8. Animation spec (make these genuinely innovative)

All motion uses spring physics, runs on graphicsLayer where possible, and honours the system "remove animations" setting.

- Logo launch: the emblem strokes draw on, fills with a glass shimmer, then its radar ring expands and becomes the first radar sweep of the map (shared-element continuity from splash to map).
- Cloak Dissolve transition: screen changes dissolve through a noise-threshold shader with a glowing edge, as if the old screen is being cloaked away (API 33+ shader, crossfade and scale fallback).
- Lobby orbit: player avatars orbit a central join-code orb under spring physics; new players snap in magnetically; join-code characters flip like a split-flap board.
- Countdown: giant digits morph into each other, the screen-edge glow pulses in sync with a haptic heartbeat.
- Proximity heartbeat: a vignette and haptic pulse whose rate maps to distance band, so players feel the chase without looking.
- Catch moment: a shockwave ripple across the map, a brief chromatic-aberration glitch, avatars ghost-fade into spectral state, a custom haptic composition.
- Glass sheets: detented bottom sheets with rubber-band overscroll, blur strength and corner radius animating with drag progress.
- Buttons: squash and stretch on press, specular highlight shifting under the finger, hold-to-confirm ring fill for destructive actions.
- Number tickers and rolling score counters; leaderboard rows that physically swap places.
- Predictive back gestures with a scaled preview, throughout.
- Results replay: the match is replayed on the map as glowing comet trails from stored on-device data, scrubbable.
- Micro-interactions everywhere: toggles, chips, tabs, list inserts, empty states, error states.

## 11. Repo, CI and releases

- Use gh to create a public repository named CloakRun under my account (check gh auth status first; if not logged in, tell me to run gh auth login). Description: "CloakRun - premium real-world hide and seek. Made by Arnav Games." Add topics. Initial commit, main branch, .gitignore (including keystores, google-services.json, local.properties), README with banner, feature list, screenshots placeholder and a Download badge, CHANGELOG.md, and no open-source licence unless I ask (all rights reserved, Arnav Games).
- Branching: main is always releasable. Work on feature branches, merge via PR (self-review), squash with Conventional Commit titles.
- Signing: generate a release keystore locally with keytool into a folder outside the repo. Never commit it. Store it in GitHub Actions secrets with gh secret set (keystore as base64, alias, passwords, plus google-services.json as base64). Tell me to back the keystore up somewhere safe: if it is lost, existing users can never update.
- CI (ci.yml): on pull requests run lint, unit tests, rules tests (emulator) and a debug build.
- Release (release.yml): on tags v*, build a signed release APK and AAB, name them CloakRun-vX.Y.Z.apk, compute SHA-256, generate release notes from commits since the last tag, and publish a GitHub Release with the APK, AAB and checksums attached.
- scripts/release.sh patch|minor|major: bumps versionName and versionCode, updates CHANGELOG.md, commits, tags and pushes. Cut a release after every completed feature batch or phase, not only at the end, so people can always download the latest build from the repo's Releases page.
- In-app updater (a sideload product flavor only; a separate play flavor omits it because Play restricts the install-packages permission): check the latest GitHub Release, compare versions, show a glass update sheet with the changelog, download with DownloadManager, verify the published SHA-256, then hand off to the system installer through a FileProvider.
- GitHub Pages landing page (free): a premium one-page site with the logo, a short looping demo placeholder, a big Download button pointing at the latest release, and the Android App Links assetlinks.json.

## 12. Working method and standing rules

Phases (finish, test and release each before the next):

- Phase 0: repo, CI, docs, design tokens, brand assets, Firebase setup guide.
- Phase 1: design system, custom icons, glass components, Design Gallery.
- Phase 2: map style, location service, play-area drawing, Simulation Mode.
- Phase 3: Firebase auth, lobby, join by code and QR, role assignment, rules and tests.
- Phase 4: core gameplay loop (Classic): head-start, hunt, fuzzed positions, BLE and GPS tagging, results.
- Phase 5: powers, cache points, extra modes, replay.
- Phase 6: animation polish, haptics, sound, performance and accessibility pass, anti-cheat hardening.
- Phase 7: release hardening, docs, landing page, privacy policy draft.

Definition of done for every feature: it builds, tests pass, lint is clean, it has previews or screenshots, it respects reduced-motion and TalkBack, it does not break the performance budget, and it is documented in the changelog.

Quality bars: pure-Kotlin game engine with deterministic unit tests (zone shrink, fuzzing, role assignment, tag validation); screenshot tests for key screens; Baseline Profile and Macrobenchmark for startup and map scrolling; accessibility (TalkBack labels, large text, contrast, colour-blind-safe palettes); RTL-ready layouts; all strings in resources.

If you have no emulator or device, build and test what you can, then give me a short real-world test checklist (docs/TEST_PLAN.md) to run on my phone.

STANDING RULE: at the end of EVERY response, append a section titled "Level-up backlog" containing at least 25 NEW ideas I have not seen yet, grouped under: UI, Map, Animation, Gameplay, Backend, Security and anti-cheat, Performance, Social and retention, Polish. Tag each with effort (S, M, L) and impact (Low, Med, High). Never repeat an idea already listed. Save them to docs/IDEAS.md, and finish by recommending the top 3 to build next and asking which I want.
