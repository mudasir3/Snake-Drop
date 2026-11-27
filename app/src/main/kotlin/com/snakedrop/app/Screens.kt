@file:Suppress("MaxLineLength")

package com.snakedrop.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snakedrop.game.Difficulty
import com.snakedrop.game.Level
import com.snakedrop.game.Levels
import com.snakedrop.game.ReplayPlayer
import com.snakedrop.platform.ProfileStore
import com.snakedrop.platform.RaceBackend
import com.snakedrop.platform.RaceEntry
import com.snakedrop.render.Palette

@Composable
private fun Menu(
    title: String,
    scroll: Boolean = false,
    content: @Composable () -> Unit,
) {
    val mod = if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(
        Modifier.fillMaxSize().background(Palette.Ink).statusBarsPadding().then(mod).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ArcadeText(title, 26, Palette.Snake)
        content()
    }
}

@Composable
fun HomeScreen(
    best: Int,
    onPlay: () -> Unit,
    onHow: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Palette.Ink).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        ArcadeText("SNAKE", 40, Palette.Snake)
        ArcadeText("DROP", 40, Palette.Apple)
        ArcadeText("BEST  $best", 14, Palette.TextDim)
        ArcadeButton("PLAY", onClick = onPlay)
        ArcadeButton("HOW TO PLAY", primary = false, onClick = onHow)
    }
}

@Composable
fun ModeScreen(
    onLevels: () -> Unit,
    onEndless: () -> Unit,
    onMulti: () -> Unit,
    onBack: () -> Unit,
) {
    Menu("MODES") {
        ArcadeButton("LEVELS", onClick = onLevels)
        ArcadeButton("ENDLESS", onClick = onEndless)
        ArcadeButton("MULTIPLAYER", onClick = onMulti)
        ArcadeButton("BACK", primary = false, onClick = onBack)
    }
}

@Composable
fun DifficultyScreen(
    title: String,
    onPick: (Difficulty) -> Unit,
    onBack: () -> Unit,
) {
    Menu(title) {
        ArcadeButton("SMALL") { onPick(Difficulty.SMALL) }
        ArcadeText("Slower snake, slower drops, roomy arena.", 11, Palette.TextDim)
        ArcadeButton("MEDIUM") { onPick(Difficulty.MEDIUM) }
        ArcadeText("The standard game.", 11, Palette.TextDim)
        ArcadeButton("HARD") { onPick(Difficulty.HARD) }
        ArcadeText("Fast snake, fast drops, starts with rubble, arena shrinks early.", 11, Palette.TextDim)
        ArcadeButton("BACK", primary = false, onClick = onBack)
    }
}

@Composable
fun MultiplayerScreen(
    online: Boolean,
    onDaily: () -> Unit,
    onVersus: () -> Unit,
    onBack: () -> Unit,
) {
    Menu("MULTIPLAYER") {
        ArcadeButton("DAILY RACE", onClick = onDaily)
        ArcadeText(
            if (online) "Same board for everyone today. Best score wins." else "Same board for everyone today. Demo rivals: offline mode.",
            11,
            Palette.TextDim,
        )
        ArcadeButton("VS BOT (LIVE)", onClick = onVersus)
        ArcadeText("Clear rows to send garbage rows. Last one standing wins. Human opponents need a server.", 11, Palette.TextDim)
        ArcadeButton("BACK", primary = false, onClick = onBack)
    }
}

@Composable
fun LevelSelectScreen(
    store: ProfileStore,
    onPick: (Level) -> Unit,
    onBack: () -> Unit,
) {
    Menu("LEVELS", scroll = true) {
        Levels.all.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { level ->
                    val unlocked = level.id == 1 || store.stars(level.id - 1) > 0
                    Box(Modifier.weight(1f)) { LevelTile(level, store.stars(level.id), unlocked) { onPick(level) } }
                }
            }
        }
        ArcadeButton("BACK", primary = false, onClick = onBack)
    }
}

@Composable
private fun LevelTile(
    level: Level,
    stars: Int,
    unlocked: Boolean,
    onClick: () -> Unit,
) {
    val border = if (unlocked) Palette.AccentAlt else Palette.Grid
    val mod = Modifier.fillMaxWidth().height(96.dp).border(2.dp, border).let { if (unlocked) it.clickable(onClick = onClick) else it }
    Column(mod.padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
        ArcadeText("${level.id}", 20, if (unlocked) Palette.TextHi else Palette.TextDim)
        ArcadeText(level.difficulty.name.take(1) + " " + level.goalType.name.take(3) + " " + level.target, 10, Palette.TextDim)
        ArcadeText(if (unlocked) "*".repeat(stars).padEnd(3, '-') else "LOCK", 12, Palette.Snake)
    }
}

@Composable
fun ResultScreen(
    result: SessionResult,
    store: ProfileStore,
    race: RaceBackend,
    hasNextLevel: Boolean,
    onAgain: () -> Unit,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    val mode = result.session.mode
    var board by remember { mutableStateOf<List<RaceEntry>?>(null) }
    var verified by remember { mutableStateOf<Boolean?>(null) }
    if (mode is GameMode.Daily) {
        LaunchedEffect(result) {
            val replay = result.session.run.finish()
            verified = ReplayPlayer.verify(replay)
            board = race.submit(mode.day, "YOU", result.score, replay.encode())
        }
    }
    Menu(
        when (result.outcome) {
            Outcome.LEVEL_WON -> "LEVEL CLEAR"
            Outcome.MATCH_WON -> "YOU WIN"
            Outcome.MATCH_LOST -> "YOU LOSE"
            else -> "GAME OVER"
        },
        scroll = true,
    ) {
        ArcadeText("SCORE ${result.score}   BEST ${store.bestScore}", 14)
        if (result.outcome == Outcome.LEVEL_WON) ArcadeText("*".repeat(result.stars).padEnd(3, '-'), 28, Palette.Snake)
        if (mode is GameMode.Versus) ArcadeText("BOT ${result.session.opponentScore}", 14, Palette.Apple)
        if (mode is GameMode.Daily) {
            DailyBoard(board, verified, race.isOnline)
        }
        if (result.outcome == Outcome.LEVEL_WON && hasNextLevel) ArcadeButton("NEXT LEVEL", onClick = onNext)
        ArcadeButton(
            if (result.outcome == Outcome.LEVEL_WON) "REPLAY" else "AGAIN",
            primary = result.outcome != Outcome.LEVEL_WON,
            onClick = onAgain,
        )
        ArcadeButton("HOME", primary = false, onClick = onHome)
    }
}

@Composable
private fun DailyBoard(
    board: List<RaceEntry>?,
    verified: Boolean?,
    online: Boolean,
) {
    ArcadeText(if (online) "DAILY RACE" else "DAILY RACE (OFFLINE DEMO RIVALS)", 12, Palette.AccentAlt)
    if (verified == false) ArcadeText("Replay did not verify. Score not counted.", 11, Palette.Danger)
    if (board == null) {
        ArcadeText("...", 12, Palette.TextDim)
        return
    }
    board.forEachIndexed { i, e ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ArcadeText("${i + 1}. ${e.name}", 12, if (e.isYou) Palette.Snake else Palette.TextHi)
            ArcadeText("${e.score}", 12, if (e.isYou) Palette.Snake else Palette.TextHi)
        }
    }
}
