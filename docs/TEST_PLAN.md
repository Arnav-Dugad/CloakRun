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

## v0.3.0: map, location and play areas

Go outside to a park or campus you know, in daylight, with a friend.

1. Open CloakRun and tap **Design a play area**. Allow location when asked. The camera flies in from above and tilts
   while the 3D buildings rise.
2. Check the map looks right where you are:
   - roads glow violet and cyan
   - footpaths show as dashed lines
   - parks are softly green
   - buildings are extruded
   - the attribution button (i) is visible at the bottom left
3. Walk about 50 m. Your dot follows within a few seconds and its halo matches the GPS accuracy.
4. **Circle:** tap the map to move the centre, then drag the radius slider. The area readout updates live. Below
   2 ha or above 3 km² it says why and Save is disabled.
5. **Polygon:** tap to add corners, then drag a corner. Make the edges cross and check the warning appears. Undo
   and Clear work.
6. Tap **Save area**, leave the screen and come back. The saved area is restored.
7. Tap the map-style button. It cycles automatic, night and day with a smooth crossfade. On automatic after sunset,
   the night style is used.
8. Turn on battery saver and walk for a minute. Tracking continues (it slows down, which is intended).
9. Turn off location in system settings and reopen the screen. The map still works and a notice explains why your
   dot is missing.
10. Accessibility: with TalkBack on, the shape tabs, slider and buttons are all announced with their state.

Debug builds only:

1. Open **Simulation**, tap **Spawn bots** and set **10x**. Bots walk along streets and paths and stay inside the
   area.
2. Turn on **Walk me**. Your dot starts moving by itself.
3. Turn on **Match service**. A notification appears, with the CloakRun glyph in the status bar and a Stop action,
   and the "Sharing your fuzzy zone" pill appears at the top of the app. Turn it off and both disappear.
