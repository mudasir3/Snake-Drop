package com.snakedrop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.snakedrop.game.Levels
import com.snakedrop.platform.OfflineRaceBackend
import com.snakedrop.platform.PrefsProfileStore
import com.snakedrop.platform.ProfileStore
import com.snakedrop.platform.RaceBackend

class MainActivity : ComponentActivity() {
    private val race = OfflineRaceBackend()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = PrefsProfileStore(applicationContext)
        setContent { SnakeDropApp(store, race) }
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

    data class Play(val mode: GameMode) : Screen

    data class Result(val result: SessionResult) : Screen
}

@Composable
fun SnakeDropApp(
    store: ProfileStore,
    race: RaceBackend,
) {
    var screen by remember { mutableStateOf<Screen>(if (store.tutorialDone) Screen.Home else Screen.Tutorial) }

    fun go(s: Screen) {
        screen = s
    }

    fun finish(r: SessionResult) {
        val m = r.session.mode
        if (m !is GameMode.Versus && r.score > store.bestScore) store.bestScore = r.score
        if (m is GameMode.LevelRun && r.outcome == Outcome.LEVEL_WON) store.setStars(m.level.id, r.stars)
        go(Screen.Result(r))
    }

    when (val s = screen) {
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
            LevelSelectScreen(store, onPick = { go(Screen.Play(GameMode.LevelRun(it))) }, onBack = { go(Screen.Modes) })
        }
        Screen.EndlessPick -> {
            BackHandler { go(Screen.Modes) }
            DifficultyScreen("ENDLESS", { go(Screen.Play(GameMode.Endless(it))) }, { go(Screen.Modes) })
        }
        Screen.Multi -> {
            BackHandler { go(Screen.Modes) }
            MultiplayerScreen(
                online = race.isOnline,
                onDaily = { go(Screen.Play(GameMode.Daily(todayEpochDay()))) },
                onVersus = { go(Screen.VersusPick) },
                onBack = { go(Screen.Modes) },
            )
        }
        Screen.VersusPick -> {
            BackHandler { go(Screen.Multi) }
            DifficultyScreen("BOT LEVEL", { go(Screen.Play(GameMode.Versus(it))) }, { go(Screen.Multi) })
        }
        is Screen.Play -> {
            BackHandler { go(Screen.Home) }
            GameScreen(s.mode, onFinished = ::finish, onQuit = { go(Screen.Home) })
        }
        is Screen.Result -> {
            val mode = s.result.session.mode
            val nextLevel = (mode as? GameMode.LevelRun)?.let { Levels.byId(it.level.id + 1) }
            BackHandler { go(Screen.Home) }
            ResultScreen(
                result = s.result,
                store = store,
                race = race,
                hasNextLevel = nextLevel != null,
                onAgain = { go(Screen.Play(mode)) },
                onNext = { nextLevel?.let { go(Screen.Play(GameMode.LevelRun(it))) } },
                onHome = { go(Screen.Home) },
            )
        }
    }
}
