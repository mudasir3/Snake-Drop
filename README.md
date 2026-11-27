# Snake Drop

Android game (Kotlin, Jetpack Compose Canvas). Rules: `docs/rules.md`. Architecture: `docs/Architecture.md`.

## Status
- Engine (`:engine-core`, `:game-snakedrop`, `:tools`): rules, 3 difficulties, 12 goal levels, replays (deterministic, verifiable), versus rules with garbage rows, bot opponent. 100+ unit tests and a 10,000-run fuzzer.
- App: first-run animated tutorial, Levels (stars, unlock chain), Endless (Small/Medium/Hard), Daily Race (same seed for everyone, replay verified locally), Live Vs Bot.
- Multiplayer limits: there is no server. Daily Race uses `OfflineRaceBackend` (demo rivals) and Vs Bot runs locally. Human opponents need a `RaceBackend` / `MatchTransport` implementation over a network.
- Build and lint clean (debug + release/R8); NOT yet run on a device or emulator. Level targets, star pars and bot difficulty are untuned first estimates.
- Not yet: sound assets, Press Start 2P font, real ads/billing, Remote Config, revive UI, haptics, particles.
- `applicationId` is TBD until the software-house name is chosen; it must never change after the first upload.

## Verify (no Android SDK needed)
    ./gradlew ktlintCheck detekt test :tools:runFuzzer -PpureOnly   # engine only
    ./gradlew ktlintCheck detekt test :app:assembleDebug :app:assembleRelease   # needs Android SDK (local.properties: sdk.dir)
