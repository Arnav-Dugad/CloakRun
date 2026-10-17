# Real-world test plan

Automated tests cover rules, game logic and UI. This checklist covers what only a real phone, outdoors, can prove.
Run the relevant section after each release and note the device, Android version and result.

## v0.1.0: foundation

1. Download `CloakRun-v0.1.0.apk` from the Releases page. Check its SHA-256 against the release notes.
2. Install it (allow "install unknown apps" for your browser or file manager when asked).
3. Launcher icon: the cloak emblem fills the mask on your launcher shape (circle, squircle or teardrop) with no
   clipping of the eye or the pixels.
4. Themed icons (Android 13+): long-press the home screen, open Wallpaper and style, and enable **Themed icons**.
   The CloakRun icon becomes a single-tone cloak silhouette.
5. Cold start on Android 12+: the emblem outline draws on, the cloak fills, the ring expands, "MADE BY ARNAV GAMES"
   shows at the bottom, then the splash fades into the About screen.
6. Cold start on Android 8 to 11 (if available): a static emblem on midnight, then the About screen.
7. Accessibility: in Settings > Accessibility, turn on **Remove animations**. The rings on the About screen stop and
   the emblem appears without a bounce. Turn it off again: the rings resume without restarting the app.
8. TalkBack: swipe through the About screen. The emblem reads "CloakRun emblem", the wordmark reads "CloakRun", and
   each card title is announced as a heading.
9. Large text: set font size to the largest. All text wraps and nothing is cut off.
10. Gesture navigation: back from the About screen exits to the launcher cleanly.

Report anything odd as a GitHub issue with a screenshot or screen recording.
