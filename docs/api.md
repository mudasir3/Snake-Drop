# Network Protocol Restraints
- **Custom backend:** None in v1.0. Never create REST/GraphQL/WebSocket endpoints. Never add Retrofit/OkHttp directly.
- **Network access:** Only through approved SDKs (AdMob, UMP, Billing, Play Games, Firebase Analytics/Crashlytics/Remote Config). All use HTTPS.
- **Offline-first:** Make the game fully playable with zero connectivity and default config. Treat every network call as optional and fail silently.
- **Boundary:** Isolate SDKs behind interfaces in `:platform`. Expose suspend functions returning `Result`-style sealed types. Never let SDK types leak into `:game-snakedrop` or `:app` UI.
- **Threading:** Call SDK APIs off Main where supported. Never block the game loop on a network call.
- **Timeouts:** Remote Config fetch timeout 10 s; minimum fetch interval 12 h (release), 0 (debug). Ad load timeout 15 s.
- **Retries:** At most 1 retry for ad load with 30 s backoff. No retries for analytics (SDK handles).
- **Deep links:** None in v1.0. Never register intent filters.
- **Privacy:** Send no custom identifiers. Attach no free text. Respect UMP consent for ad requests.

# Endpoint Contract Mappings
Internal gateway interfaces (`:platform`) — the contract surface for agents:
- **`AdsGateway`:**
  - `suspend fun initialize(consent: ConsentState): AdResult<Unit>`
  - `suspend fun loadRewarded(): AdResult<Unit>`; `suspend fun showRewarded(activity): AdResult<RewardGranted>`
  - `suspend fun loadInterstitial(): AdResult<Unit>`; `suspend fun showInterstitial(activity): AdResult<Unit>`
  - Ad unit keys: `AD_REWARDED_REVIVE`, `AD_INTERSTITIAL_GAMEOVER` (debug = Google test IDs; release from `local.properties`).
- **`BillingGateway`:**
  - Product: `remove_ads` (in-app, non-consumable). 
  - `suspend fun queryAdsRemoved(): BillingResult<Boolean>`; `suspend fun purchaseRemoveAds(activity): BillingResult<Unit>`; `suspend fun restore(): BillingResult<Boolean>`.
  - Always acknowledge a successful purchase.
- **`PlayGamesGateway`:**
  - `suspend fun signInSilently(): GamesResult<Unit>`
  - `suspend fun submitScore(score: Long): GamesResult<Unit>` → leaderboard `LB_HIGH_SCORE`.
  - `suspend fun unlock(id: AchievementId): GamesResult<Unit>`.
  - Achievements (10): `ACH_FIRST_CHUNK`, `ACH_FIRST_CLEAR`, `ACH_DOUBLE_CLEAR`, `ACH_TRIPLE_CLEAR`, `ACH_QUAD_CLEAR`, `ACH_SCORE_1000`, `ACH_SCORE_5000`, `ACH_APPLES_50_RUN`, `ACH_RUNS_25`, `ACH_REVIVE_ONCE`.
  - Replace placeholders with Play Console IDs from `local.properties`/resources; never hardcode.
- **`AnalyticsGateway`:** `fun log(event: AnalyticsEvent)`. Allowed events:
  - `app_open`
  - `run_start`
  - `run_end` {`score:int`, `apples:int`, `rows_cleared:int`, `duration_s:int`, `cause:wall|self|crushed|no_spawn|no_space`}
  - `first_row_clear` {`seconds_since_run_start:int`}
  - `revive_offered`, `revive_accepted`
  - `interstitial_shown`
  - `remove_ads_purchased`
  - Never add per-apple/per-tick events. Never add free-text params.
- **`RemoteConfigGateway`:** `suspend fun fetchAndActivate(): ConfigResult<Unit>`; `fun snapshot(): RemoteTunables` (already clamped).

Remote Config keys (default → allowed range; clamp every value; apply at run start only):
- **`snake_speed_start`:** 6.0 → [4.0, 8.0].
- **`snake_speed_step`:** 0.25 → [0.0, 0.5].
- **`snake_speed_max`:** 11.0 → [8.0, 14.0].
- **`chunk_fall_ms`:** 150 → [80, 300].
- **`chunk_fall_ms_fast`:** 110 → [60, 200] (must be ≤ `chunk_fall_ms`).
- **`fast_fall_after_apples`:** 20 → [10, 60].
- **`arena_shrink_threshold`:** 6 → [4, 8].
- **`arena_min_rows`:** 7 → [5, 10].
- **`shrink_warning_ms`:** 1500 → [800, 3000].
- **`interstitial_every_n`:** 3 → [2, 6].
- **`interstitial_min_gap_s`:** 90 → [60, 300].
- **`interstitial_free_runs`:** 3 → [0, 6].
- **`revive_rows_cleared`:** 2 → [1, 4].
- **`ads_enabled`:** true → boolean (kill switch).
- **Non-tunable (never add keys):** `width`, `arena_rows`, `well_rows`, `start_len`, `apples_per_chunk`, `chunk_len`.

Persistence contract (`SettingsRepository`): `val settings: Flow<SettingsState>`; `suspend fun update(transform: (SettingsState) -> SettingsState)`; `suspend fun recordBest(score: Int)`. Keys defined in `schema.md`.

# Strict Error-Handling Formats
- **Result type:** Return `sealed interface PlatformResult<out T>` = `Ok(value)` | `Err(error: PlatformError)`. Never throw across module boundaries.
- **Error shape:** `data class PlatformError(val code: ErrorCode, val message: String, val cause: Throwable? = null)`. `message` ≤ 120 chars, no PII.
- **ErrorCode enum:** `NETWORK`, `TIMEOUT`, `NOT_READY`, `NO_FILL`, `CONSENT_REQUIRED`, `USER_CANCELED`, `BILLING_UNAVAILABLE`, `ALREADY_OWNED`, `NOT_SIGNED_IN`, `INVALID_CONFIG`, `UNKNOWN`.
- **Ads failure:** On `NO_FILL`/`TIMEOUT`/`NOT_READY`, skip the ad and proceed. Never block Retry. For revive, hide the button if no rewarded ad is loaded.
- **Billing failure:** On `BILLING_UNAVAILABLE`/`NETWORK`, show a one-line non-blocking message ("Purchase unavailable. Try again later."). On `ALREADY_OWNED`, call `restore()` and set `ads_removed`. On `USER_CANCELED`, do nothing.
- **Play Games failure:** On `NOT_SIGNED_IN`/`NETWORK`, drop the call silently; never queue scores offline in v1.0.
- **Config failure:** On `INVALID_CONFIG` or fetch failure, keep in-app defaults. Log once at debug level. Never surface to the user.
- **Crash policy:** Report non-fatal errors to Crashlytics only for `UNKNOWN`, with `code` and a short tag. Never include user data.
- **UI messages:** Use plain strings from `strings.xml`; max 1 toast/snackbar per 5 s; never show raw exception text.
- **Logging:** Log with tag `SnakeDrop` at `WARN` for recoverable errors, `ERROR` for `UNKNOWN`. Strip in release.
- **Contract tests:** Provide fake gateways (`FakeAdsGateway`, etc.) in `:platform` test fixtures. Test every ErrorCode path for ads, billing, and config.
