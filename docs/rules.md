# Project Overview
- **Product:** Snake Drop. One-thumb arcade puzzle. Steer snake, eat apples; every 4th apple detaches the last 4 tail segments as a chunk that falls into a well; full rows clear; tall stack shrinks the arena.
- **Platform:** Android only. Kotlin, Jetpack Compose, single Activity. minSdk 24. targetSdk = level Play requires on release day.
- **Spec authority:** PRD 1 → `requirements.md`. Never invent rules. On conflict or gap, stop and ask; do not guess.
- **Audience:** 13+. Never set child-directed flags. Never add content aimed at minors.
- **Assets:** Generate everything in code (Canvas shapes, synthesized PCM audio). Never add purchased or third-party art/audio.
- **Release scope:** v1.0 = one endless mode, local high score, Play Games leaderboard + 10 achievements, rewarded revive, Remove Ads IAP. Everything else is out of scope (see `requirements.md`).
- **Shared engine:** `:engine-*` modules are shared with Color Breaker. Keep them game-agnostic.

# Code Style & Conventions
- **Language:** Kotlin only. Never add Java files.
- **Formatter:** Enforce ktlint + detekt. Run both before every commit. Zero warnings policy.
- **Naming:** Types `PascalCase`; functions/vars `camelCase`; constants `SCREAMING_SNAKE_CASE`; packages lowercase, no underscores.
- **Imports:** Never use wildcard imports.
- **Immutability:** Prefer `val`, `data class`, `copy()`. Mutate only inside pooled/preallocated hot-path structures (`IntArray`, ring buffers) and document why.
- **Function size:** Keep functions ≤ 40 lines, files ≤ 400 lines. Split by responsibility.
- **Constants:** Put every tunable in `GameConfig`. Never hardcode numbers in logic. Cite the PRD value in a one-line comment.
- **Comments:** Explain why, not what. KDoc every public API in `:engine-*` and `:game-snakedrop`.
- **Coroutines:** Inject dispatchers. Never use `GlobalScope`. Never call blocking I/O on Main.
- **Compose:** Keep composables stateless where possible. Hoist state to `GameViewModel`. Preview every screen composable.
- **Errors:** Return typed results (`sealed interface`) from platform wrappers. Never swallow exceptions silently; log via `Logger` (stripped in release).
- **Commits:** Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`). One logical change per commit.

# Structural Hard Rules
- **Pure engine:** Never import `android.*`, `androidx.*`, or Compose in `:engine-core` or `:game-snakedrop`. These are plain Kotlin/JVM so tests run without a device.
- **Dependency direction:** `:app` → `:game-snakedrop` → `:engine-core`. `:app` → `:engine-render`, `:engine-audio`, `:platform`. Never invert. Never let `:engine-*` depend on `:game-snakedrop`.
- **Deterministic step:** `SnakeDropEngine.step(state)` mutates the state in place (no per-tick allocation). No I/O, no clock reads, no randomness except the seeded RNG in the state.
- **Determinism:** Never call `System.currentTimeMillis`, `nanoTime`, `Math.random`, or unseeded `Random` inside simulation code. Use `XorShift64` from `:engine-core`. Keep simulation reproducible from (seed + input log).
- **Fixed timestep:** Advance the simulation with a fixed-step accumulator at `SIM_HZ = 120`. Clamp frame delta to 250 ms to survive pauses/spikes. Drawing runs at display rate.
- **No per-frame allocation:** Never allocate objects, lambdas, strings, or boxed numbers in `step()` or draw code. Reuse `Path`, `Paint`, arrays, and a particle pool (cap 300).
- **No blur effects:** Build glow from 2–3 layered strokes. Never use `RenderEffect`/blur (slow on low-end phones).
- **Config-driven tuning:** Read tunables through `GameConfig`, overridden by Remote Config values clamped to safe ranges (`api.md`). Never trust a remote value unclamped.
- **Fixed invariants:** Never make `APPLES_PER_CHUNK` (4), `CHUNK_LEN` (4), `START_LEN` (4), grid width (12), arena rows (10), well rows (9) remotely tunable. They define the length invariant (4 + 4 − 4 = 4).
- **Ads policy:** Never show an ad during an active run. First 3 runs ad-free. Interstitial only after every 3rd game over, ≥ 90 s apart. Rewarded revive once per run. No banners.
- **Consent order:** Initialize UMP consent before initializing ads. Never load an ad before consent state is known.
- **Dependencies:** Never add a dependency without explicit approval. Use the version catalog `gradle/libs.versions.toml`. See `tools.md`.
- **Binary assets:** Never commit png/jpg/webp/mp3/ogg/wav. Only exceptions: launcher icon as vector XML; one OFL font file + its license in `app/src/main/res/font/`.
- **Identity:** Never change `applicationId` after first Play upload. Never commit keystores or `google-services.json` (see `security.md`).
- **Main thread:** Never block Main for > 4 ms. Run AI/IO/billing/ads work off Main.
- **Process death:** Pause on `onPause`. Persist best score immediately on new best. Never resume mid-run after process death; start a fresh run.

# Common Gotchas & Anti-Patterns
- **180° reversal:** Reject a turn opposite to the current heading, including when two swipes arrive in one tick. Buffer at most 2 queued turns; validate each against the heading after the previous queued turn.
- **Apple spawn:** Spawn only on cells not occupied by the snake. If no free cell exists, end the run as a win-state (`NoSpace`), never loop forever.
- **Chunk geometry:** Preserve exact relative shape and columns of the 4 detached segments. Never rotate or re-order them. Chunks are 4-connected polyominoes (tetromino shapes).
- **Detach timing:** Trigger detach on the 4th apple eaten (counter resets after detach). Resulting snake length must be exactly 4.
- **Row clear:** Clear full 12-cell rows only. Drop rows above by whole-row gravity, not per-cell gravity. Score `100 × rows²` per lock event.
- **Arena shrink:** `arenaRows = 10 − max(0, h − 6)`, min 7, where `h` = stack height in the well. Show the 1.5 s warning pulse before the floor rises. End the run only if a snake segment is in a removed row.
- **Compose recomposition:** Never recompose per frame. Read frame state inside `Canvas` draw scope via a `State`/`MutableLongState` frame counter. Never allocate in `drawScope`.
- **Frame clock:** Drive the loop with `withFrameNanos`. Cancel it when the lifecycle is not `RESUMED`.
- **Float locale:** Never format numbers with the default locale in game UI or analytics. Use `Locale.ROOT`.
- **SoundPool:** Load synthesized buffers once at startup off Main. Never `play()` before `onLoadComplete`.
- **Interstitial gating:** Track "game overs since last interstitial" and "ms since last interstitial" in `AdPolicy`. Unit-test both gates.
- **Revive:** Clear the bottom 2 well rows, keep score, restore a legal snake state, freeze the snake for 1.5 s (no invulnerability), respawn the apple if missing. Allow once per run.
- **Remote Config defaults:** Ship in-app defaults equal to PRD values. The game must work fully offline with defaults.
- **Test seeds:** Pin seeds in every test. Never write a test that depends on wall-clock or real randomness.
- **Trademarks:** Never use "Nokia", "Tetris", "Snake" as a brand name, or any look-alike art/sound in the store listing, UI, or code identifiers exposed to users.

# Modes (added after v1.0 PRD)
- **Difficulty:** `Difficulty.SMALL/MEDIUM/HARD` presets in `Difficulty.kt`. MEDIUM equals PRD defaults. HARD adds 2 starting rubble rows and an earlier arena shrink.
- **Rubble:** Each rubble row has one 4-wide gap so one straight chunk can clear it.
- **Levels:** 12 goal levels in `Levels.kt` (APPLES, CHUNKS, ROWS, SCORE). Unlock needs 1+ star on the previous level. Par times and score targets are untuned.
- **Replay:** `RecordedRun` records accepted turns; `ReplayPlayer.verify` re-simulates to check a claimed score. Never trust a client score without it.
- **Versus:** Same seed per player. Clearing 2/3/4 rows sends 1/2/4 garbage rows; garbage cancels incoming first and lands when the next chunk locks. A stack overflow ends the run. Opponent link is the `MatchTransport` interface.
