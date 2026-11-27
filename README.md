# Snake Drop

Android game (Kotlin, Jetpack Compose Canvas). Rules: `docs/rules.md`. Architecture: `docs/Architecture.md`.

## Status
- Engine (`:engine-core`, `:game-snakedrop`, `:tools`): rules, 3 difficulties, 12 goal levels, replays (deterministic, verifiable), versus rules with garbage rows, bot opponent. 100+ unit tests and a 10,000-run fuzzer.
- App: first-run animated tutorial, Levels (stars, unlock chain), Endless (Small/Medium/Hard), Daily Race (same seed for everyone, replay verified locally), Live Vs Bot.
- Multiplayer limits: there is no server. Daily Race uses `OfflineRaceBackend` (demo rivals) and Vs Bot runs locally. Human opponents need a `RaceBackend` / `MatchTransport` implementation over a network.
- Build and lint clean (debug + release/R8); NOT yet run on a device or emulator. Level targets, star pars and bot difficulty are untuned first estimates.
- UI: animated retro backdrop themed per difficulty, hard-shadow buttons with haptics, screen crossfades, particles/shake/flash/danger vignette from engine events, animated stars and score count-up, interactive practice board in the tutorial.
- Ads: AdMob (Google Mobile Ads 24.8.0, UMP consent). Interstitial per `AdPolicy`; rewarded ad revives once per run (not in Vs Bot). Uses Google's sample IDs unless you set your own.
- Not yet: sound assets, Press Start 2P font, Remove Ads purchase, Remote Config, Firebase/real multiplayer backend.
- `applicationId` is TBD until the software-house name is chosen; it must never change after the first upload.

## Verify (no Android SDK needed)
    ./gradlew ktlintCheck detekt test :tools:runFuzzer -PpureOnly   # engine only
    ./gradlew ktlintCheck detekt test :app:assembleDebug :app:assembleRelease   # needs Android SDK (local.properties: sdk.dir)

## AdMob setup
Put your real IDs in `~/.gradle/gradle.properties` (never in the repo):

    admobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
    admobInterstitialId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
    admobRewardedId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX

Debug builds always work with Google's sample IDs. A release build fails unless the IDs are set (or you pass `-PallowTestAds` for a local test build, never to publish). Ads SDK is pinned to 24.8.0: 24.9+ is built with a newer Kotlin than this project's 2.0.21.
Before publishing: fill the Play Console Data safety form for ads, and set up your UMP consent message in the AdMob console.
