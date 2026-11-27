@file:Suppress("MaxLineLength")

package com.snakedrop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.snakedrop.game.AdPolicy
import com.snakedrop.game.Levels
import com.snakedrop.platform.AdMobGateway
import com.snakedrop.platform.AdsGateway
import com.snakedrop.platform.OfflineRaceBackend
import com.snakedrop.platform.PrefsProfileStore
import com.snakedrop.platform.ProfileStore
import com.snakedrop.platform.RaceBackend
import com.snakedrop.render.ArcadeTheme

class MainActivity : ComponentActivity() {
    private val race = OfflineRaceBackend()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = PrefsProfileStore(applicationContext)
        val ads = AdMobGateway(this, BuildConfig.ADMOB_INTERSTITIAL_ID, BuildConfig.ADMOB_REWARDED_ID)
        ads.start()
        setContent { SnakeDropApp(store, race, ads) }
    }
}

private sealed interface Screen {
    data object Tutorial : Screen

    data object Home : Screen

    data object Modes : Screen

    data object Levels : Screen

    data object EndlessPick : Screen

    data object Multi : Screen

    data object VersusPick : Screen

    data class Play(val session: Session) : Screen

    data class Result(val result: SessionResult) : Screen
}

@Composable
fun SnakeDropApp(
    store: ProfileStore,
    race: RaceBackend,
    ads: AdsGateway,
) {
    var screen by remember { mutableStateOf<Screen>(if (store.tutorialDone) Screen.Home else Screen.Tutorial) }
    val policy =
        remember {
            AdPolicy(
                totalGameOvers = store.totalGameOvers,
                gameOversSinceInterstitial = store.gameOversSinceInterstitial,
                lastInterstitialMs = store.lastInterstitialMs,
            )
        }

    fun go(s: Screen) {
        screen = s
    }

    fun play(mode: GameMode) = go(Screen.Play(Session(mode)))

    fun finish(r: SessionResult) {
        val m = r.session.mode
        if (m !is GameMode.Versus && r.score > store.bestScore) store.bestScore = r.score
        if (m is GameMode.LevelRun && r.outcome == Outcome.LEVEL_WON) store.setStars(m.level.id, r.stars)
        val showAd = policy.onGameOver(System.currentTimeMillis())
        store.totalGameOvers = policy.totalGameOvers
        store.gameOversSinceInterstitial = policy.gameOversSinceInterstitial
        store.lastInterstitialMs = policy.lastInterstitialMs
        if (showAd) ads.showInterstitial { go(Screen.Result(r)) } else go(Screen.Result(r))
    }

    fun revive(r: SessionResult) {
        var earned = false
        ads.showRewarded(
            onReward = { earned = true },
            onClosed = { if (earned && r.session.run.revive()) go(Screen.Play(r.session)) },
        )
    }

    Box(Modifier.fillMaxSize()) {
        AppBackdrop(themeFor(screen))
        Crossfade(targetState = screen, animationSpec = tween(220), label = "screen") { s ->
            when (s) {
                Screen.Tutorial -> {
                    TutorialScreen {
                        store.tutorialDone = true
                        go(Screen.Home)
                    }
                }
                Screen.Home -> HomeScreen(store.bestScore, onPlay = { go(Screen.Modes) }, onHow = { go(Screen.Tutorial) })
                Screen.Modes -> {
                    BackHandler { go(Screen.Home) }
                    ModeScreen({ go(Screen.Levels) }, { go(Screen.EndlessPick) }, { go(Screen.Multi) }, { go(Screen.Home) })
                }
                Screen.Levels -> {
                    BackHandler { go(Screen.Modes) }
                    LevelSelectScreen(store, onPick = { play(GameMode.LevelRun(it)) }, onBack = { go(Screen.Modes) })
                }
                Screen.EndlessPick -> {
                    BackHandler { go(Screen.Modes) }
                    DifficultyScreen("ENDLESS", { play(GameMode.Endless(it)) }, { go(Screen.Modes) })
                }
                Screen.Multi -> {
                    BackHandler { go(Screen.Modes) }
                    MultiplayerScreen(
                        online = race.isOnline,
                        onDaily = { play(GameMode.Daily(todayEpochDay())) },
                        onVersus = { go(Screen.VersusPick) },
                        onBack = { go(Screen.Modes) },
                    )
                }
                Screen.VersusPick -> {
                    BackHandler { go(Screen.Multi) }
                    DifficultyScreen("BOT LEVEL", { play(GameMode.Versus(it)) }, { go(Screen.Multi) })
                }
                is Screen.Play -> {
                    BackHandler { go(Screen.Home) }
                    GameScreen(s.session, onFinished = ::finish, onQuit = { go(Screen.Home) })
                }
                is Screen.Result -> {
                    val mode = s.result.session.mode
                    val nextLevel = (mode as? GameMode.LevelRun)?.let { Levels.byId(it.level.id + 1) }
                    val canRevive = mode !is GameMode.Versus && policy.canOfferRevive(s.result.session.state, ads.isRewardedReady())
                    BackHandler { go(Screen.Home) }
                    ResultScreen(
                        result = s.result,
                        store = store,
                        race = race,
                        hasNextLevel = nextLevel != null,
                        canRevive = canRevive,
                        onRevive = { revive(s.result) },
                        onAgain = { play(mode) },
                        onNext = { nextLevel?.let { play(GameMode.LevelRun(it)) } },
                        onHome = { go(Screen.Home) },
                    )
                }
            }
        }
    }
}

private fun themeFor(screen: Screen): ArcadeTheme =
    when (screen) {
        is Screen.Play -> ArcadeTheme.of(screen.session.difficulty)
        is Screen.Result -> ArcadeTheme.of(screen.result.session.difficulty)
        else -> ArcadeTheme.Midnight
    }
