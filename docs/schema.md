# Database Architecture
- **Server DB:** None in v1.0. Never add Firestore, Realtime Database, Room, or SQLite in v1.0.
- **Persistent store:** Jetpack DataStore (Preferences) for settings, best score, and counters. Single file `snakedrop_prefs`.
- **Runtime store:** In-memory `GameState` (plain Kotlin, flat primitive arrays). Never persist mid-run state.
- **Remote config:** Firebase Remote Config key-value (see `api.md`). Read-only from the app.
- **Future (v1.1, do not build now):** Daily seeded run reuses Ghost Race backend (`walls/{wallId}/scores/{uid}` in Firestore). Keep `GameState` seedable (`seed: Long`) so this needs no rewrite.
- **Access pattern:** Read DataStore once at startup into `SettingsState`. Write on change only. Never read DataStore inside the game loop.
- **Threading:** Access DataStore from `Dispatchers.IO` via `SettingsRepository`. Expose `Flow<SettingsState>` to the ViewModel.

# Entity-Relationship Schemas
Persistent (DataStore Preferences keys):
- **`best_score`:** Int, default 0. Update immediately on new best.
- **`runs_total`:** Int, default 0.
- **`game_overs_since_interstitial`:** Int, default 0. Reset after each interstitial.
- **`last_interstitial_ms`:** Long (epoch ms), default 0. Gate ≥ 90 s. Non-simulation use only.
- **`ads_removed`:** Boolean, default false. Re-verified via Billing on launch.
- **`first_run_done`:** Boolean, default false. Set after overlay completes.
- **`sound_on`:** Boolean, default true. **`music_on`:** Boolean, default true. **`vibration_on`:** Boolean, default true.
- **`control_style`:** Enum string `SWIPE|BUTTONS`, default `SWIPE`.
- **`screen_shake_on`:** Boolean, default true. **`reduce_motion`:** Boolean, default false.
- **`consent_ready`:** Boolean (mirror of UMP result), default false.
- **`achievements_progress`:** Int bitmask (10 bits), default 0.

Runtime `GameState` (flat arrays; zero allocation per step):
- **`seed`:** Long. **`rngState`:** Long (XorShift64 state).
- **`tick`:** Long — total fixed sim ticks.
- **`arena`:** `IntArray(12 * 10)` — 0 empty, 1 snake body, 2 snake head, 3 apple. Index = `row * 12 + col`; row 0 = top.
- **`snakeCells`:** ring buffer `IntArray(12 * 10)` of cell indices, head→tail order; `snakeHead`, `snakeLen` ints.
- **`heading`:** Int 0..3 (UP,RIGHT,DOWN,LEFT). **`turnQueue`:** `IntArray(2)` + `turnQueueLen`.
- **`snakeSpeed100`:** Int (ticks/s × 100, avoids floats: 600 → 6.00).
- **`moveAccum`:** Int sim-tick accumulator toward next snake move.
- **`appleIndex`:** Int cell index. **`applesEaten`:** Int. **`applesSinceDetach`:** Int 0..3.
- **`well`:** `IntArray(12 * 9)` — 0 empty, 1..6 locked cell color id. Index = `row * 12 + col`; row 0 = top of well, row 8 = floor.
- **`chunk`:** `IntArray(8)` (4 × [rowOffset, col]) + `chunkActive` Boolean, `chunkTopRow` Int, `chunkColorId` Int, `chunkFallAccumMs` Int.
- **`chunksLanded`:** Int (drives color = `chunksLanded % 6 + 1`).
- **`wellHeight`:** Int 0..9 (derived, cached). **`arenaRows`:** Int 7..10.
- **`shrinkWarnMsLeft`:** Int (0 = no warning). **`pendingArenaRows`:** Int.
- **`score`:** Int. **`rowsClearedTotal`:** Int. **`bestRowClear`:** Int (max rows in one lock).
- **`reviveUsed`:** Boolean. **`invulnMsLeft`:** Int.
- **`phase`:** Enum `RUNNING|PAUSED|ENDED`. **`endCause`:** Enum `NONE|WALL|SELF|CRUSHED|NO_SPAWN|NO_SPACE`.

Relationships:
- **Snake ↔ arena:** `snakeCells` is the source of truth; `arena` is a derived render/collision grid rebuilt incrementally on each move.
- **Chunk → well:** On detach, `chunk` is derived from the last 4 `snakeCells`; on lock, its cells copy into `well` with `chunkColorId`.
- **Well → arena:** `wellHeight` determines `arenaRows`; arena rows above `arenaRows` are the playable region (floor rises from the bottom of the arena).
- **State → events:** Each `step()` emits `Events` (see `Architecture.md`); events never mutate state.

# Indexing & Performance Bounds
- **Cell index:** Always `row * WIDTH + col` with `WIDTH = 12`. Centralize in `Grid.idx(row, col)`. Never compute inline elsewhere.
- **Array sizes:** Arena 120 ints, well 108 ints, snake ring 120 ints. All preallocated once per run.
- **Step cost budget:** `step()` ≤ 0.2 ms on a mid-range phone. No heap allocation. No boxing. No `List`/`Map` in hot path.
- **Row-clear scan:** Scan at most 9 rows × 12 cols per lock event.
- **Collision check:** O(1) via `arena[idx]`.
- **Memory budget:** `GameState` ≤ 4 KB. Particle pool ≤ 300 objects. Total heap growth over a 10-minute run: 0.
- **Persistence bounds:** Write DataStore only on change or on run end. Never more than 1 write per second.
- **Hash for determinism tests:** Compute a 64-bit FNV-1a hash over `arena`, `well`, `snakeCells[0..len]`, `score`, `rngState`. Use in replay tests only.
- **Numeric rules:** Use Int/Long for all simulation math. Never use Float/Double in `step()`. Speed stored ×100.
- **Config clamps:** Apply ranges from `api.md` before writing to `GameConfig`.
