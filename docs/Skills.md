---
name: snake-drop-rule-change
description: Execute a spec-driven change to a Snake Drop gameplay rule (snake, chunk, well, arena, scoring, ad policy). Trigger when a task modifies behavior in :game-snakedrop or tunables in GameConfig. Enforces PRD alignment, TDD, determinism, and tuning safety.
---

# Prerequisites
- **Read first:** `rules.md`, `requirements.md` (find the FR-xx id), `schema.md` (state layout), `testing.md` (TDD protocol).
- **Confirm spec:** Locate the governing FR-xx. If none exists or it conflicts with the task, stop and ask the user. Never improvise rules.
- **Check invariants:** Confirm the change keeps `START_LEN=4`, `APPLES_PER_CHUNK=4`, `CHUNK_LEN=4`, width 12, arena 10 rows, well 9 rows, and snake length returning to exactly 4 after detach.
- **Environment:** JDK + Gradle wrapper working (`./gradlew --version`). Baseline green: `./gradlew :game-snakedrop:test :engine-core:test`.
- **Branch:** Create `feat/<fr-id>-<slug>` or `fix/<slug>` from main.

# Sequential Execution Steps
1. **Frame the change:** Write one sentence naming the FR-xx, the current behavior, the new behavior, and the PRD citation.
2. **Write failing tests:** Add or extend tests in `game-snakedrop/src/test/kotlin/` using a pinned seed (`XorShift64(seed = 1234L)`). Include the happy path plus at least 2 edge cases from `testing.md`. Run `./gradlew :game-snakedrop:test` and confirm failure for the right reason.
3. **Implement minimally:** Modify only the relevant `*Rules.kt` or `Scoring.kt`. Keep `step()` deterministic (same seed + inputs = same `stateHash()`). Add no allocation in hot paths. Route any new number through `GameConfig` with a PRD citation comment.
4. **Run unit tests:** `./gradlew :game-snakedrop:test :engine-core:test`. Fix until green. Never weaken a test to pass.
5. **Run the fuzzer:** `./gradlew :tools:runFuzzer`. Require 10,000 random runs with zero exceptions and zero invariant violations (see `testing.md`).
6. **Check determinism:** Run the same seed + input log twice. Assert identical final `GameState` hash. Fail the task if they differ.
7. **Lint:** `./gradlew ktlintCheck detekt`. Fix all findings.
8. **Update docs:** If behavior changed, update `requirements.md` (FR text), `schema.md` (fields), and PRD open-questions list in the same commit. If a tunable was added, add its Remote Config key and clamp range to `api.md`.
9. **Smoke on device:** `./gradlew :app:installDebug`; play 3 runs; verify no dropped frames and the changed behavior visually.
10. **Commit:** Conventional Commit referencing FR-xx. One logical change.
11. **Report:** State FR-xx touched, tests added, fuzzer result, any assumption made, and any open question that needs the user.
