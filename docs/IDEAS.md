# Ideas backlog

Every response that ships work adds at least 25 new ideas, never repeating an earlier one. Tags: effort S, M or L;
impact Low, Med or High. Ideas move to `CHANGELOG.md` when built.

## Seed backlog (from the brief)

- **UI:** tilt-reactive glass specular; dynamic-island-style in-match status pill; live-activity-style lock-screen
  notification with timer and role; adaptive layouts for tablets and foldables; per-team accent theming that
  recolours the whole UI; glass bottom nav that morphs into a sheet; ultra-minimal Stealth HUD with a dimmed screen;
  pocket mode (screen off, haptics and audio only); bold typographic results screen with animated stats; custom
  animated illustrations for empty states; haptic-synced onboarding.
- **Map:** animated heat shimmer on the last-known zone; contour-line elevation glow; transit and landmark pulses for
  orientation; compass-ring HUD around the player dot; 3D tilted chase cam that follows heading; rain, fog and
  night-sky effects tied to real weather and time; breadcrumb replay with speed colouring; AR Radar overlay (ARCore)
  with arrows toward the last-known zone; offline tile caching for the chosen play area; custom 3D landmark models
  for famous buildings.
- **Animation:** metaball merging when teammates' zones overlap; cloak shimmer shader on the player's own dot; glass
  shatter transition on elimination; confetti of glass shards on victory; physics-based avatar pile-up on the results
  podium; liquid morph between role cards; gyroscope parallax on the home screen; animated gradient mesh backgrounds;
  shared-element avatar flights between screens; loading states as animated radar sweeps.
- **Gameplay:** hider decoy drones; seeker scanner drone cooldown; team voice quick-callouts with push-to-talk;
  daily and weekly challenges; seasonal map themes; rank ladder with badges; custom rule builder; Last Standing
  royale ladder; asymmetric roles (Scout, Trickster, Hunter, Medic); ghost spectators who drop one-time hints;
  practice mode against bots.
- **Backend:** Remote Config live-tuned constants; per-match quota guard that degrades update rates; compressed
  delta position updates; signed match tokens against forged joins; stale-match garbage collection; Crashlytics keys
  per match phase; privacy-preserving analytics funnel; staged-rollout flag for new modes; offline-first lobby with
  reconnect resume; rules unit-test coverage gate in CI.
- **Security and anti-cheat:** BLE-proximity tag proofs; sensor-fusion plausibility checks; mock-location and
  rooted-device risk scoring for the host; per-match integrity report; time-skew detection against server
  timestamps; replay hash so results cannot be edited.
- **Performance:** adaptive blur quality by device tier; shader fallbacks tested on low-end devices; map layer
  budgeting; startup under 1 second with a Baseline Profile; battery-aware sampling; frame-time overlay in debug.
- **Social and retention:** shareable glass results cards; friends list by code; crews with a shared trophy shelf;
  match history and stats; highlight reels from replays; achievements with custom badge art; next-match widget.
- **Polish:** onboarding that teaches by playing a 60-second simulated round; custom sound pack; localisation with
  RTL; accessibility modes (high contrast, reduced motion, one-handed); logo easter eggs; a cinematic About Arnav
  Games credits scene.

## Batch 1 (2026-10-17, after Phase 0)

### UI
1. **Sunlight-adaptive glass.** The ambient light sensor raises glass opacity and text weight in direct sun. (M, High)
2. **Scratch-off role reveal.** Your role card sits under frost that you rub away with a finger, with a grainy
   haptic texture. (S, Med)
3. **Cutout timer ring.** The match timer draws as a thin ring around the front camera cutout, using the display
   cutout API. (M, Med)
4. **Handedness mirroring.** Learns which thumb you use from touch positions and mirrors primary controls to that
   side. (M, Med)

### Map
5. **Building line-of-sight.** Seeker radar is occluded by real building footprints, so hiding behind a block
   genuinely works. (L, High)
6. **Sun shadows.** Building shadows computed from the real sun position darken the map; hiders gain a small
   fuzz bonus in shadow. (M, Med)
7. **Street-snapped play areas.** Tap a spot and get a proposed play area whose edges snap to real streets and park
   boundaries. (M, High)
8. **Hazard cutouts.** Water, highways and rail corridors are automatically carved out of the play area and drawn
   as no-go hatching. (M, High)

### Animation
9. **Shrink wavefront.** When the zone contracts, the border ripples inward as a wave with a chromatic edge. (S, Med)
10. **Code orb split.** Copying the join code splits the orb into its six characters, which then re-merge. (S, Low)
11. **Seeker sweep wake.** Seeker cones leave faint fading wedges behind them, so you can read where they looked.
    (S, Med)

### Gameplay
12. **Witness posts.** An eliminated player stands still for 20 seconds to become a one-shot camera that reveals a
    30 m radius. (M, High)
13. **Bounty hider.** The last hider standing carries a bounty that grows with survival time and pays out to the
    catcher. (S, Med)
14. **Real weather modifiers.** Live local rain shortens seeker radar; fog adds noise to fuzzy zones. (M, Med)
15. **Tracker scent.** A seeker power that shows only the direction a hider's zone drifted over the last 60 seconds.
    (S, High)

### Backend
16. **Polyline-encoded play areas.** Store polygons with the encoded polyline algorithm to cut document size about
    five times. (S, Med)
17. **Host migration.** If the host vanishes for 60 seconds, the longest-connected player claims referee through a
    rules-checked transaction. (L, High)
18. **Deterministic match seed.** All spawns and random events derive from a shared seed plus the event log, so any
    client can re-verify the host. (M, High)

### Security and anti-cheat
19. **Commit-reveal positions.** Hiders commit a hash of their cell each epoch; a disputed tag is settled by revealing
    one commitment, without exposing history. (L, High)
20. **GNSS sanity checks.** Compare satellite count and signal strength from GnssStatus with the claimed accuracy to
    flag spoofed fixes. (M, Med)
21. **BLE relay detection.** A tag requires the RSSI curve to match the seeker's own approach motion, so a relayed
    advertisement fails. (L, High)

### Performance
22. **Overlay frame governor.** When frame time exceeds 12 ms for 30 frames, decorative overlays (fog, trails, grain)
    are shed first, in a fixed order. (M, High)
23. **Shader warm-up.** Draw every AGSL effect once off-screen during the countdown, so the first catch never janks.
    (S, Med)
24. **Screen-off batching.** Use fused location `maxUpdateDelay` batching while the screen is off to wake the CPU less.
    (S, Med)

### Social and retention
25. **Ghost race.** Replay your own trail against a friend's earlier trail in the same area. (M, Med)
26. **Private territory map.** An on-device heatmap of the cells you have survived in across matches; it never
    syncs. (M, Med)
27. **Animated invite cards.** Share a glass invite image with the QR code, start time and an area thumbnail. (S, High)

### Polish
28. **Role haptic signatures.** Hiders feel soft low taps and seekers feel crisp high ticks, so you know your role by
    touch. (S, Med)
29. **Night-vision palette.** A red-shifted UI for Night Run that preserves dark adaptation outdoors. (S, Med)
30. **Seasonal launcher icons.** Swap the launcher icon through activity aliases for seasonal events. (S, Low)

## Batch 2 (2026-10-17, after Phases 1 and 2)

### UI
31. **Corner loupe.** While you drag a polygon corner, a glass magnifier shows the street under your finger. (S, High)
32. **Contextual thumb action.** The primary action morphs to what matters now; for example Tag appears only when a
    hider's Bluetooth ID is in range. (M, High)
33. **Spoken glance.** One long-press reads your status aloud (role, time left, zone distance) for pocket play. (M, Med)

### Map
34. **Walkability shading.** Streets are shaded by pedestrian friendliness (footways, low-traffic roads) to guide the
    host's area design. (M, High)
35. **Private-land exclusion.** OpenStreetMap areas tagged `access=private` or `access=no` are hatched, excluded from
    cache spawns and warned about. (M, High)
36. **Lit-street overlay.** At night, streets tagged `lit=yes` glow brighter, and unlit ones are flagged as less safe.
    (M, High)
37. **Area thumbnails.** A snapshot of each saved area becomes its glass card thumbnail in lobby lists. (S, Med)

### Animation
38. **Energy-line ignition.** When an area becomes valid, its border ignites from one point and races around the
    loop. (S, Med)
39. **Glowing footsteps.** Moving players leave a short trail of fading footstep dots. (S, Low)
40. **Validity breath.** The tool sheet breathes once when the area flips between valid and invalid. (S, Low)

### Gameplay
41. **Park snap.** Tap inside a park and the play area snaps to that park's real boundary polygon. (M, High)
42. **Hider base.** The host places a base where hiders start, and the head-start distance is enforced. (S, Med)
43. **Size coach.** Recommends a play-area size from the lobby size and mode. (S, High)

### Backend
44. **Area codes.** Encode a play area as a short base32 code to share it outside a lobby. (S, Med)
45. **Tile budget.** Count tile fetches per session, respect OpenFreeMap's bulk limits, and switch to a fallback style
    host from Remote Config. (S, High)
46. **Write coalescing.** Merge bursts of lobby edits (ready, role, settings) into one Firestore write per 500 ms.
    (S, Med)

### Security and anti-cheat
47. **Simulated-fix rejection.** The release match engine rejects any fix whose source is Simulated, as defence in
    depth. (S, High)
48. **Route plausibility.** Compare a trail with the walkable graph; long straight runs through buildings flag
    teleporting or spoofing. (M, High)
49. **Tracking-loss signal.** If an OEM battery killer stops the match service, the player shows as "signal lost"
    instead of silently vanishing. (S, High)

### Performance
50. **Projected-path cache.** Re-project overlay geometry only when the camera moves, not on every frame. (S, Med)
51. **Map pixel ratio.** Performance mode renders the map at 0.75x pixel density. (S, Med)
52. **Style prewarm.** Fetch both styles' glyph ranges at start so day and night crossfades are instant. (S, Low)

### Social and retention
53. **Favourite places.** Save several named play areas, such as "Cubbon Park loop". (S, High)
54. **Area ratings.** After a match, players rate the area for fun and safety, shared with their crew. (M, Med)
55. **Scout routes.** The host's scouting walk is saved as a suggested route for new players. (M, Low)

### Polish
56. **Glass attribution.** The map attribution becomes a glass chip that matches the interface, and stays visible.
    (S, Low)
57. **Limit haptics.** A soft tick when a drag crosses the minimum or maximum area. (S, Med)
58. **Sky gradient.** Above the tilted horizon, a subtle sky follows the real sun altitude. (M, Med)

## Batch 3 (2026-10-17, after Phase 3)

### UI
59. **Find-me buzz.** Tap a player in the lobby to make their phone buzz and flash, so people find each other at the
    meeting point. (S, High)
60. **Pre-game checklist chips.** Each lobby row shows battery, GPS accuracy, Bluetooth and notification status as
    small icons, so the host sees who is really ready. (M, High)
61. **Read the code aloud.** One button speaks the join code with the NATO alphabet ("Kilo, Seven, Quebec"). (S, Med)

### Map
62. **Gathering meter.** The lobby shows how many players are already at the play area, as distance bands only, never
    positions. (M, High)
63. **Meeting point pin.** The host drops a meet-up pin inside the area; the lobby shows it with a walking time. (S, High)
64. **Briefing flyover.** During the briefing, the camera circles the area boundary and lingers on each no-go zone.
    (M, High)

### Animation
65. **Role card flip.** The briefing role card flips like a playing card, with a holographic sheen driven by tilt. (S, Med)
66. **Join ripple.** When someone joins, a ripple travels from the code orb to their new orbit slot. (S, Low)
67. **Removal dissolve.** A removed player's avatar cloak-dissolves out of the orbit. (S, Low)

### Gameplay
68. **Run it back.** A rematch button keeps the area and rotates who seeks, with no re-briefing for unchanged roles.
    (S, High)
69. **Session balancer.** Role shuffles weigh each player's catches and escapes this session to keep teams even. (M, Med)
70. **Late spectators.** People who join after the start enter as spectators with the ghost-eye view instead of being
    turned away. (M, Med)

### Backend
71. **Stale lobby sweep.** On launch, the app ends or forgets any lobby of yours that has expired, so nothing lingers.
    (S, Med)
72. **Offline join queue.** A join that fails for lack of signal retries when the network returns, with a "waiting
    for signal" state. (S, Med)
73. **Rules coverage report.** CI uploads the emulator's rules coverage report so untested rule branches are visible.
    (S, Med)

### Security and anti-cheat
74. **Code-guessing throttle.** Join-code lookups are rate limited per account with a rules-checked counter document.
    (M, High)
75. **Host handoff.** If the host is offline for 60 seconds, the longest-connected player can take over as host; the
    rules check presence timestamps. (L, High)
76. **Name filter.** An on-device, multi-language word filter for player names, on top of the character rules. (S, High)

### Performance
77. **Background listener pause.** Lobby listeners detach after two minutes in the background and reattach on
    resume, saving reads. (S, Med)
78. **Roster in one document.** A compact roster map inside the match document lets clients use one listener instead
    of two. (M, Med)
79. **QR bitmap cache.** The QR code is rendered once per code to a bitmap instead of drawing modules each frame.
    (S, Low)

### Social and retention
80. **Crews.** A standing friends group with its own code; hosting offers to invite the whole crew in one share.
    (M, High)
81. **Lobby poster.** A shareable image card ("6 players, Cubbon Park, tonight") for group chats. (S, Med)
82. **Crew streaks.** Count consecutive weeks a crew has played together. (S, Low)

### Polish
83. **Smart paste.** The join screen spots a code or invite link on the clipboard and offers "Paste K7Q2XM". (S, High)
84. **Typing haptics.** Each code tile ticks as it fills, and a full code plays a short confirm cascade. (S, Low)
85. **Team halo.** Once roles are assigned, each avatar gets a thin ring in its team colour. (S, Low)
86. **Live landing page.** The website shows the latest version and its highlights from the GitHub Releases API.
    (S, Low)
