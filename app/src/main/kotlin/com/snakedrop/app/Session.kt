package com.snakedrop.app

import com.snakedrop.game.BotOpponent
import com.snakedrop.game.DailySeed
import com.snakedrop.game.Difficulty
import com.snakedrop.game.GameState
import com.snakedrop.game.Level
import com.snakedrop.game.Levels
import com.snakedrop.game.MatchController
import com.snakedrop.game.MatchResult
import com.snakedrop.game.Phase
import com.snakedrop.game.RecordedRun
import com.snakedrop.game.RunSetup
import com.snakedrop.game.SnakeDropEngine

private const val MS_PER_DAY = 86_400_000L
private const val FX_CAPACITY = 128

/** What the player chose to play. */
sealed interface GameMode {
    data class Endless(val difficulty: Difficulty) : GameMode

    data class LevelRun(val level: Level) : GameMode

    /** Same board for everyone on [day]; one shared seed. */
    data class Daily(val day: Long) : GameMode

    /** Live head-to-head against the local bot. */
    data class Versus(val difficulty: Difficulty) : GameMode
}

/** Current UTC day number, used as the daily-race key. */
fun todayEpochDay(): Long = System.currentTimeMillis() / MS_PER_DAY

/** Terminal states a session can reach. */
enum class Outcome { PLAYING, GAME_OVER, LEVEL_WON, MATCH_WON, MATCH_LOST }

/** One play session: owns the run (recorded), and the match + bot in versus. Driven by the game loop. */
class Session(val mode: GameMode) {
    private val setup: RunSetup =
        when (mode) {
            is GameMode.Endless -> RunSetup(System.nanoTime(), mode.difficulty)
            is GameMode.LevelRun -> RunSetup(System.nanoTime(), mode.level.difficulty, mode.level.id)
            is GameMode.Daily -> RunSetup(DailySeed.forEpochDay(mode.day), Difficulty.MEDIUM)
            is GameMode.Versus -> RunSetup(System.nanoTime(), mode.difficulty)
        }

    val run = RecordedRun(setup)

    /** Difficulty used for theming. */
    val difficulty: Difficulty get() = setup.difficulty

    private val fxTypes = IntArray(FX_CAPACITY)
    private val fxArgs = IntArray(FX_CAPACITY)

    /** Engine events produced since the UI last called [clearFx]. */
    var fxCount: Int = 0
        private set

    fun fxType(i: Int): Int = fxTypes[i]

    fun fxArg(i: Int): Int = fxArgs[i]

    fun clearFx() {
        fxCount = 0
    }

    val bot: BotOpponent? = (mode as? GameMode.Versus)?.let { BotOpponent(setup.seed, it.difficulty) }
    private val match = bot?.let { MatchController(run.state, it) }
    val state: GameState get() = run.state
    val isLive: Boolean get() = match != null

    /** Advances one fixed tick. */
    fun step() {
        if (match != null) match.step() else run.step()
        val ev = state.events
        for (i in 0 until ev.count) {
            if (fxCount < FX_CAPACITY) {
                fxTypes[fxCount] = ev.type(i)
                fxArgs[fxCount] = ev.arg(i)
                fxCount++
            }
        }
        state.events.clear()
        bot?.state?.events?.clear()
        val m = mode
        if (m is GameMode.LevelRun && Levels.isComplete(state, m.level)) SnakeDropEngine.win(state)
    }

    /** Queues a swipe turn. */
    fun turn(dir: Int) {
        run.queueTurn(dir)
    }

    /** Where the session stands. */
    fun outcome(): Outcome =
        when {
            match?.result == MatchResult.WON -> Outcome.MATCH_WON
            match?.result == MatchResult.LOST -> Outcome.MATCH_LOST
            state.phase == Phase.WON -> Outcome.LEVEL_WON
            state.phase == Phase.ENDED -> Outcome.GAME_OVER
            else -> Outcome.PLAYING
        }

    /** The opponent's last reported score (versus only). */
    val opponentScore: Int get() = bot?.state?.score ?: 0
}
