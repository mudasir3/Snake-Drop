# Local Environment Commands (Setup, Build, Lint)
Run from repo root. Use the Gradle wrapper only; never a system Gradle.

- **Verify toolchain:** `./gradlew --version` (expect JDK 17+ configured for Android Gradle Plugin).
- **Setup (first time):** Open in Android Studio → sync. Copy `local.properties.example` → `local.properties`. Place `google-services.json` in `app/` (never commit it).
- **Build debug APK:** `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/`.
- **Install on device:** `./gradlew :app:installDebug` (or `adb install -r <apk>`).
- **Build release bundle:** `./gradlew :app:bundleRelease` → signed AAB via Play App Signing upload key (see `security.md`).
- **Unit tests (all):** `./gradlew test`.
- **Unit tests (engine only):** `./gradlew :game-snakedrop:test :engine-core:test`.
- **Instrumented tests:** `./gradlew :app:connectedDebugAndroidTest` (device/emulator attached).
- **Fuzzer:** `./gradlew :tools:runFuzzer` (10,000 random runs; must pass before merge).
- **Lint (format):** `./gradlew ktlintCheck`; autofix `./gradlew ktlintFormat`.
- **Lint (static):** `./gradlew detekt`.
- **Android lint:** `./gradlew :app:lintDebug`.
- **Pre-commit gate:** `./gradlew ktlintCheck detekt test` (must be green).
- **Size check:** `./gradlew :app:bundleRelease` then inspect AAB size; fail if > 15 MB.
- **Clean:** `./gradlew clean`.
- **Logs:** `adb logcat -s SnakeDrop` (tag all logs `SnakeDrop`; stripped in release).
- **Frame check:** `adb shell dumpsys gfxinfo <applicationId> framestats` after a 5-minute run; require no frame > 33 ms.

# Allowed Tool List
- **Allowed shell:** `./gradlew *`, `adb *`, `git status|diff|log|add|commit|checkout|switch|branch|restore`, `ls`, `cat`, `grep`, `rg`, `find`, `sed -n`, `mkdir`.
- **Allowed file ops:** Read/Write/Edit inside the repo only. Create files with Write/Edit, not shell redirects.
- **Allowed web:** Read official docs only (developer.android.com, kotlinlang.org, developers.google.com, firebase.google.com, support.google.com/googleplay).
- **Ask first:** `git push`, `git rebase`, `git reset --hard`, deleting files, editing `gradle/libs.versions.toml`, editing `AndroidManifest.xml` permissions, editing signing config.
- **Never allowed:** `rm -rf` outside `build/`, `curl | sh`, `sudo`, editing files outside the repo, reading `~/.ssh` or keystore passwords, `git push --force`, committing secrets.

# Third-Party Package Policies
- **Source of truth:** Declare every library in `gradle/libs.versions.toml`. Never hardcode versions in module build files.
- **Approval rule:** Never add a dependency without explicit user approval. State purpose, size impact, license, and alternatives before asking.
- **Pre-approved (v1.0):** Kotlin stdlib + coroutines; AndroidX (core-ktx, activity-compose, lifecycle, navigation-compose, datastore-preferences); Jetpack Compose (BOM-managed); Google Mobile Ads SDK; Google UMP SDK; Play Billing Library; Play Games Services v2; Firebase BoM with Analytics, Crashlytics, Remote Config.
- **Test-only pre-approved:** JUnit 4, kotlin-test, kotlinx-coroutines-test, Turbine (flows), Compose UI test, ktlint plugin, detekt plugin.
- **Forbidden:** Game engines (libGDX, Unity, Godot), image/audio asset libraries, analytics/ad SDKs beyond the list above, social login SDKs, any SDK that collects precise location, deprecated Firebase Dynamic Links, reflection-heavy DI frameworks.
- **License policy:** Allow Apache-2.0, MIT, BSD, SIL OFL (fonts). Reject GPL/AGPL/unknown licenses.
- **Size budget:** Keep AAB < 15 MB. Reject a dependency adding > 500 KB without justification.
- **Update policy:** Bump versions only in a dedicated `chore(deps):` commit with tests green. Never bump during feature work.
- **Data Safety sync:** Adding an SDK requires updating the Play Data Safety answers; note it in the PR.
