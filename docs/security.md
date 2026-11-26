# Authentication & Encryption Models
- **User accounts:** None. Never add sign-up, login, or user profiles in v1.0.
- **Play Games sign-in:** Use Play Games Services v2 automatic sign-in only. Never store tokens, IDs, or display names beyond the SDK.
- **Firebase identity:** v1.0 uses Analytics/Crashlytics/Remote Config only. Never add Firebase Auth, Firestore, or Realtime Database in v1.0.
- **Local storage:** DataStore (settings, best score, flags) holds non-sensitive data only. Never store PII, ad IDs, purchase tokens, or credentials.
- **Purchases:** Verify entitlement through Play Billing `queryPurchasesAsync` on each launch. Acknowledge purchases. Cache `adsRemoved` boolean in DataStore for speed, but re-verify on launch. Known limitation: no server-side receipt validation in v1.0; do not gate anything valuable beyond ad removal on it.
- **Transport:** HTTPS only. Set `network_security_config.xml` with `cleartextTrafficPermitted="false"`. Never pin certificates for SDK traffic.
- **Backups:** Set `android:allowBackup="false"` or explicit `dataExtractionRules` excluding DataStore. Never back up purchase flags.
- **Release hardening:** Enable R8 minify + resource shrinking for release. Keep ProGuard rules in `app/proguard-rules.pro`; add keep-rules only for SDKs that require them.
- **Signing:** Use Play App Signing. Keep the upload keystore outside the repo. Never print passwords in logs.
- **Manifest:** Set `android:exported="false"` on every component except `MainActivity`. Declare only `INTERNET`, `ACCESS_NETWORK_STATE`, `com.google.android.gms.permission.AD_ID`. Never request location, contacts, camera, microphone, or storage permissions.
- **Child safety:** Treat audience as 13+. Never set `tagForChildDirectedTreatment=true`. Set ad content rating max to match 13+. Never serve personalized-ad exceptions in EEA/UK without UMP consent.

# Restricted Files & Sensitive Directories
- **Never read, print, or commit:** `*.jks`, `*.keystore`, `keystore.properties`, `local.properties`, `google-services.json`, `.env*`, `*.pem`, `service-account*.json`, `play-publisher*.json`.
- **Gitignore (enforce):** `local.properties`, `keystore.properties`, `*.jks`, `*.keystore`, `google-services.json`, `.env*`, `build/`, `.gradle/`, `.idea/`, `*.aab`, `*.apk`.
- **Secrets handling:** Read signing/API values from `local.properties` or environment variables via Gradle `providers`. Never hardcode.
- **Ad IDs:** Use Google test ad unit IDs in `debug`. Real ad unit IDs live in `release` buildConfig fields from `local.properties`. AdMob App ID in the manifest is allowed in the repo (not secret) but use a debug placeholder.
- **Test devices:** Register test device IDs only in debug. Never ship them in release.
- **Do-not-edit:** `app/build/`, `**/build/generated/`, `gradle/wrapper/*` (only via `gradle wrapper`), `docs/*.md` rule files without user intent.
- **Logging:** Strip all logs in release. Never log ad IDs, purchase tokens, device identifiers, or raw SDK payloads.
- **Agent boundary:** Never open, summarize, or transmit contents of any file in the restricted list. If a task needs one, ask the user to provide the specific non-secret value.

# Data Ingestion Guardrails
- **Remote Config:** Treat every value as untrusted. Parse defensively; on parse failure use the in-app default. Clamp to the ranges in `api.md`. Apply only at the start of a run. Never let remote values change fixed invariants (width, lengths, apples-per-chunk).
- **Remote kill switch:** Support `ads_enabled=false`. Never support remote code execution or remote-defined rules beyond numeric tunables.
- **Touch input:** Accept only `Dir` values from the gesture detector. Ignore multi-touch beyond the first pointer. Never trust coordinates outside the board bounds.
- **Intents/deep links:** v1.0 declares none. Never add `intent-filter` for custom schemes or App Links in v1.0. Ignore unexpected intent extras.
- **Share card:** Write only to `cache/share/` and expose via `FileProvider` with a scoped path. Delete files older than 24 h. Never share from external storage.
- **Third-party SDK data:** Collect only what Analytics/Ads/Crashlytics collect by default. Never add custom user identifiers. Never attach score history or free text to events.
- **Event payloads:** Allow only integers, booleans, and enumerated strings from `api.md`. Cap string length at 40 chars.
- **Consent:** Initialize ads only after UMP reports consent obtained or not required. Respect later changes via the Settings privacy link.
- **Privacy docs:** Keep a public privacy policy URL on the store listing. Keep the Play Data Safety form identical to the SDKs actually present. Update both when adding an SDK.
- **Supply chain:** Pin versions through the catalog. Review release notes before upgrading ad/analytics SDKs. Reject any new dependency not approved (see `tools.md`). Verify Gradle wrapper checksum is committed.
- **Dependency scan:** Run `./gradlew dependencies` before release and inspect for unexpected transitive SDKs.
