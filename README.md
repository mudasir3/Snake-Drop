# Snake Drop

Android game (Kotlin, Jetpack Compose Canvas). Rules: `docs/rules.md`. Architecture: `docs/Architecture.md`.

## Status
- `:engine-core`, `:game-snakedrop`, `:tools` (pure JVM): implemented and tested.
- `:app`, `:engine-render`, `:engine-audio`, `:platform`: not started.
- `applicationId` is TBD until the software-house name is chosen; it must never change after the first upload.

## Verify (no Android SDK needed)
    ./gradlew ktlintCheck detekt test :tools:runFuzzer -PpureOnly
