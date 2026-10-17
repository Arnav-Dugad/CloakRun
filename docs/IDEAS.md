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
