# Snake Drop

Android game (Kotlin, Jetpack Compose Canvas). Rules: `docs/rules.md`. Architecture: `docs/Architecture.md`.

## Status
- `:engine-core`, `:game-snakedrop`, `:tools` (pure JVM): implemented and tested.
- `:app` (Home + Game screens, swipe controls, pause, game over, best score), `:engine-render` (Canvas board), `:platform` (score store, no-op ads), `:engine-audio` (no-op): build and lint clean; NOT yet run on a device or emulator.
- Not yet: sound assets, Press Start 2P font, real ads/billing, Remote Config, revive UI, animations/particles, haptics.
- `applicationId` is TBD until the software-house name is chosen; it must never change after the first upload.

## Verify (no Android SDK needed)
    ./gradlew ktlintCheck detekt test :tools:runFuzzer -PpureOnly   # engine only
    ./gradlew ktlintCheck detekt test :app:assembleDebug :app:assembleRelease   # needs Android SDK (local.properties: sdk.dir)
