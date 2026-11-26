# Core Features
Definition of Done (DoD) for every feature: unit tests green, `ktlintCheck`+`detekt` clean, verified on one low-end, one mid, one high-end device, no frame > 33 ms in a 5-minute run, analytics event fires.

- **FR-01 Playfield:** Render 12 columns. Top 10 rows = arena. Below = 9-row well sharing the same 12 columns.
- **FR-02 Snake movement:** Start length 4. Move 1 cell per tick. Start speed 6 ticks/s; +0.25 per apple; cap 11. Forbid reversing into self. Wall or self collision ends the run (`cause=wall|self`).
- **FR-03 Controls:** Default swipe-to-turn anywhere. Optional two-button left/right turn mode (Settings). Single tap on pause icon pauses. Queue ≤ 2 turns.
- **FR-04 Apples:** Keep exactly 1 apple on board. Spawn on a random free arena cell. Eating: +1 length, +10 points, +0.25 speed (to cap).
- **FR-05 Chunk detach:** On every 4th apple, detach the last 4 segments as a chunk. Snake returns to length 4. Counter resets.
- **FR-06 Chunk fall:** Spawn chunk at top of well, same shape and columns. Fall 1 row per 150 ms (110 ms after 20 apples). Not steerable in v1.0. Lock on floor/stack contact. Award 5 points per cell on landing.
- **FR-07 Row clear:** Clear any full 12-cell well row. Drop rows above by whole rows. Score `100 × rows²` (1→100, 2→400, 3→900, 4→1600).
- **FR-08 Arena shrink:** `arenaRows = 10 − max(0, h − 6)`, min 7 (h = stack height). Show red pulsing danger line at h = 6. Warn 1.5 s before floor rises. End run (`cause=crushed`) only if a snake segment is in a removed row.
- **FR-09 No-spawn loss:** End run (`cause=no_spawn`) if a new chunk cannot spawn because the stack reaches the top of the well.
- **FR-10 Game over:** Show score, best, rows cleared. Primary = Retry. Secondary = rewarded Revive (once per run). Retry from Game Over in ≤ 2 taps and ≤ 1 s.
- **FR-11 Revive:** Rewarded ad clears bottom 2 well rows, keeps score, continues run once per run.
- **FR-12 Interstitial:** Show after every 3rd game over only, ≥ 90 s since last, never in first 3 runs, never during a run.
- **FR-13 Remove Ads:** One-time IAP `remove_ads`. Removes interstitials. Rewarded stays opt-in. Support restore.
- **FR-14 Leaderboard & achievements:** Submit best score to Play Games leaderboard. Unlock 10 achievements (list in `api.md`). Fail silently when signed out.
- **FR-15 Settings:** Sound, music, vibration toggles; control style; privacy/consent link; Remove Ads/restore; screen-shake toggle.
- **FR-16 First-run overlay:** Show 3 non-blocking prompts in the first run: "Swipe to steer", "Eat the apple", "Watch your tail fall". Dismiss on action. Never repeat.
- **FR-17 Share card:** Generate a bitmap in code (score, longest snake, best row clear) and share via Android share sheet + FileProvider.
- **FR-18 Audio:** Synthesize SFX (eat, turn tick, detach, land, row clear arpeggio, game over, revive, UI click) and one 30 s music loop from a fixed seed. Music default on, low volume, one mute toggle.
- **FR-19 Remote Config:** Tune speed curve, chunk fall speed, shrink thresholds, interstitial frequency without an update. Defaults = PRD values. Clamp every value.
- **FR-20 Analytics:** Emit events listed in `api.md`. Never emit per-apple events.
- **FR-21 Consent:** Run Google UMP flow (EEA/UK) before any ad request.
- **FR-22 Performance:** 60 fps draw on 2 GB RAM phone. Cold start < 2 s. AAB < 15 MB. Crash-free sessions ≥ 99.5%.

# User Persona Workflows
- **Nostalgic casual (25–40):** Open app → Home → tap Play (≤ 2 s) → first-run overlay → eat 4 apples → watch tail fall → fail → Retry in 1 tap.
- **Block-puzzle fan:** Play → plan tail position for the 4th apple → land chunk to complete a row → double/triple clear → see score + best on Game Over → chase best.
- **Short-video viewer (13–24):** Install from ad → Home → Play → reach first row clear within 3 runs → tap Share → post share card.
- **Returning player:** Open → Home shows best score → Play → near-miss pulls another run → hits Game Over #3 → one interstitial → Retry.
- **Revive path:** Game Over → tap "Revive (watch ad)" → rewarded ad → well bottom 2 rows cleared → run continues → no second revive offered.
- **Paying player:** Settings → Remove Ads → Play Billing → interstitials stop → Restore works after reinstall.
- **Offline player:** Airplane mode → game fully playable with default config; leaderboard/ads/analytics degrade silently.

# Scope Boundaries (In-Scope vs. Explicitly Out-of-Scope)
- **In-scope v1.0:** FR-01 → FR-22 above. One endless mode. Local high score. Play Games leaderboard + 10 achievements. Rewarded revive. Remove Ads IAP. Remote Config. Closed-test → production release.
- **Out-of-scope v1.1 (do not build):** Daily seeded run (reuses Ghost Race seed/leaderboard code), chunk nudge control (A/B test), 3 color themes.
- **Out-of-scope later (do not build):** Power apples, head-to-head snake duel, skins, story, characters, accounts, chat.
- **Out-of-scope always for v1.0:** Online multiplayer, custom backend, push notifications, banner ads, ad mediation (revisit at ≈ 10,000 DAU), levels/campaign, in-app currency, child-directed mode, iOS/KMP.
- **Open items (ask before building):** Chunk steerability (assumed no). Arena shrink vs. snake (assumed game over). Final app name/icon (trademark check). Closest 3 existing store competitors must be recorded before launch.
