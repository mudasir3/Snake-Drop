# Testing Framework Commands
- **Frameworks:** JUnit 4 + `kotlin-test` for engine; `kotlinx-coroutines-test` for async; Compose UI test for screens; Turbine for flows.
- **Run all unit tests:** `./gradlew test`.
- **Run engine tests:** `./gradlew :game-snakedrop:test :engine-core:test`.
- **Run one test class:** `./gradlew :game-snakedrop:test --tests "*ChunkRulesTest"`.
- **Run instrumented UI tests:** `./gradlew :app:connectedDebugAndroidTest`.
- **Run fuzzer:** `./gradlew :tools:runFuzzer` (10,000 seeded random runs).
- **Lint gate:** `./gradlew ktlintCheck detekt :app:lintDebug`.
- **Coverage target:** ≥ 90% line coverage on `:game-snakedrop`, ≥ 80% on `:engine-core`. Report via `./gradlew :game-snakedrop:jacocoTestReport` if Jacoco is configured.
- **Test location:** `src/test/kotlin/` mirrors main package. Name files `<Subject>Test.kt`.
- **Helpers:** Build states with `TestStates` builders (`stateWithSnake(...)`, `wellFromAscii("..")`). Prefer ASCII diagrams in tests for readability.

# Test-Driven Development Protocols
- **Red first:** Write the failing test before any engine change. Run it and confirm it fails for the intended reason. Never skip.
- **One behavior per test:** Name `given_when_then` style, e.g. `fourthApple_detachesLastFourSegments_snakeLengthIsFour`.
- **Pin determinism:** Use fixed seeds. Never depend on time, threads, or device state in engine tests.
- **Green minimal:** Implement the smallest change that passes. Refactor only with tests green.
- **Regression rule:** Every bug fix starts with a failing test reproducing it, then the fix.
- **Spec traceability:** Tag each test with the FR id in its name or a comment (`// FR-07`). Every FR in `requirements.md` needs ≥ 1 test.
- **Determinism check:** Assert two runs with the same seed and the same input sequence end with identical `stateHash()`; a different seed must differ. `step()` mutates state in place (zero allocation).
- **Replay check:** For random seeds, record the input log, replay it, and assert identical final state hash.
- **Fuzz invariants (assert after every step in fuzzer):** Snake length ≥ 1; after detach snake length == 4; no two snake cells overlap; apple not on snake; no cell both in snake and well; well rows ≤ 9 high; `arenaRows` between 7 and 10; score never decreases; no exception.
- **Ad policy tests:** Unit-test `AdPolicy` for: first 3 runs ad-free; every 3rd game over; ≥ 90 s gap; revive once per run; Remove Ads disables interstitials but not rewarded.
- **Config tests:** Feed malformed, missing, negative, NaN, and out-of-range Remote Config values; assert clamping to defaults/ranges.
- **Done gate:** A task is done only when `./gradlew ktlintCheck detekt test` and the fuzzer pass.

# Edge-Case & UI Verification Criteria
Engine edge cases (each requires a test):
- **Reversal:** Two opposite swipes inside one tick never reverse the snake into itself.
- **Turn buffer:** Third queued turn is dropped; queued turns validate against projected heading.
- **Wall/self collision:** Head into wall ends run (`wall`); into own body ends run (`self`); head moving into the cell the tail just vacated is allowed.
- **Apple spawn:** Never on snake; with 1 free cell spawns there; with 0 free cells ends run without infinite loop.
- **Detach:** Exactly on the 4th apple; counter resets; shape and columns preserved; snake length == 4 after.
- **Short snake:** If length < CHUNK_LEN at detach (impossible by invariant), assert and fail loudly in debug.
- **Chunk fall:** Locks on floor and on stack; non-contiguous drop columns fall as one rigid body.
- **Spawn blocked:** Chunk cannot spawn because stack reaches top → `no_spawn`.
- **Row clear:** 1, 2, 3, 4 simultaneous rows score 100/400/900/1600; whole-row gravity; cleared rows reduce stack height before the shrink check.
- **Arena shrink:** h ≤ 6 → 10 rows; h = 7 → 9; h = 9 → 7 (minimum); warning lasts 1.5 s; snake in removed row → `crushed`; snake elsewhere → survives; clearing rows restores arena rows.
- **Revive:** Clears exactly bottom 2 well rows, keeps score, once per run, restores legal state.
- **Speed curve:** Starts 6; +0.25 per apple; caps at 11; chunk fall switches to 110 ms after 20 apples.
- **Determinism:** Same seed + inputs → identical state hash at every step.
- **Lifecycle:** Pause/resume mid-fall does not lose or duplicate a chunk; dt spikes clamp to 250 ms.

UI/device verification (manual checklist before each release, on 1 low, 1 mid, 1 high phone):
- **Frame time:** 5-minute run with no frame > 33 ms (`gfxinfo framestats`).
- **Startup:** Cold start < 2 s to Home.
- **Retry speed:** Game Over → new run ≤ 2 taps and ≤ 1 s.
- **First run:** Overlay prompts appear in order, dismiss on action, never repeat.
- **Controls:** Swipe and button modes both work one-handed; diagonal swipes resolve to dominant axis.
- **Ads:** None in first 3 runs; interstitial only after every 3rd game over; none mid-run; rewarded revive flows correctly; Remove Ads stops interstitials; restore works after reinstall.
- **Consent:** UMP dialog appears in EEA test mode before ads load.
- **Offline:** Airplane mode → full gameplay, no crash, graceful ad/leaderboard failure.
- **Display:** Notch/cutout and gesture-nav insets respected; portrait lock; 1.3× font scale doesn't break layout.
- **Audio:** All SFX audible; mute toggles work; no clicks/pops; audio survives backgrounding.
- **Memory:** No growth over a 10-minute run (heap stable via Profiler); AAB < 15 MB.
- **Accessibility:** Icon buttons have content descriptions; Reduce Motion disables shake and pulse.
