@file:Suppress("MaxLineLength")

package com.snakedrop.game

import com.snakedrop.engine.core.XorShift64

/** Messages exchanged between the two players of a versus match. */
public sealed interface MatchMsg {
    /** The sender cleared rows and sends [rows] garbage rows to the receiver. */
    public data class Garbage(val rows: Int) : MatchMsg

    /** The sender's run ended. The receiver wins if still alive. */
    public data class Ended(val cause: EndCause) : MatchMsg

    /** Periodic status of the sender, for the opponent's mini-board. */
    public data class Progress(val score: Int, val wellHeight: Int) : MatchMsg
}

/**
 * How a match talks to the other player. A network implementation sends and receives over the wire;
 * [BotOpponent] runs the other side locally. Nothing here depends on Android.
 */
public interface MatchTransport {
    /** Called once per simulation tick before messages are read. Local opponents simulate here; network ones do nothing. */
    public fun tick()

    /** Sends [msg] to the opponent. */
    public fun send(msg: MatchMsg)

    /** Returns the next message from the opponent, or null when there is none. */
    public fun poll(): MatchMsg?
}

/** Outcome of a versus match from this player's side. */
public enum class MatchResult { ONGOING, WON, LOST }

/**
 * One player's side of a versus match. Both players use the same seed; rows cleared become garbage for the other.
 * Call [step] once per simulation tick.
 */
public class MatchController(
    public val state: GameState,
    private val transport: MatchTransport,
) {
    /** Latest known opponent score. */
    public var opponentScore: Int = 0
        private set

    /** Latest known opponent stack height. */
    public var opponentHeight: Int = 0
        private set

    /** Current outcome. */
    public var result: MatchResult = MatchResult.ONGOING
        private set

    /** Advances one tick: reads opponent messages, simulates, sends garbage and status. */
    public fun step() {
        if (result != MatchResult.ONGOING) return
        transport.tick()
        drain()
        if (result != MatchResult.ONGOING) return
        SnakeDropEngine.step(state)
        val garbage = SnakeDropEngine.takeOutgoingGarbage(state)
        if (garbage > 0) transport.send(MatchMsg.Garbage(garbage))
        if (state.phase == Phase.ENDED) {
            transport.send(MatchMsg.Ended(state.endCause))
            result = MatchResult.LOST
        } else if (state.tick % PROGRESS_EVERY_TICKS == 0L) {
            transport.send(MatchMsg.Progress(state.score, state.wellHeight))
        }
    }

    private fun drain() {
        while (true) {
            when (val m = transport.poll() ?: return) {
                is MatchMsg.Garbage -> SnakeDropEngine.receiveGarbage(state, m.rows)
                is MatchMsg.Progress -> {
                    opponentScore = m.score
                    opponentHeight = m.wellHeight
                }
                is MatchMsg.Ended -> result = MatchResult.WON
            }
        }
    }

    private companion object {
        const val PROGRESS_EVERY_TICKS = 30L
    }
}

/** Local opponent: simulates the other board on the same seed and plays it with [BotPolicy]. */
public class BotOpponent(
    seed: Long,
    difficulty: Difficulty,
    private val skillRandomOneIn: Int = DEFAULT_RANDOM_ONE_IN,
    private val decideEvery: Int = DEFAULT_DECIDE_EVERY,
) : MatchTransport {
    /** The bot's own board, exposed so the UI can draw it. */
    public val state: GameState = RunSetup(seed, difficulty).create()
    private val rng = XorShift64(seed xor BOT_SALT)
    private val inbox = ArrayDeque<MatchMsg>()
    private var endedSent = false

    override fun tick() {
        if (state.phase != Phase.RUNNING) return
        if (state.tick % decideEvery == 0L) {
            BotPolicy.decide(state, rng, skillRandomOneIn) { SnakeDropEngine.queueTurn(state, it) }
        }
        SnakeDropEngine.step(state)
        state.events.clear()
        val garbage = SnakeDropEngine.takeOutgoingGarbage(state)
        if (garbage > 0) inbox.addLast(MatchMsg.Garbage(garbage))
        if (state.phase == Phase.ENDED && !endedSent) {
            endedSent = true
            inbox.addLast(MatchMsg.Ended(state.endCause))
        } else if (state.tick % PROGRESS_EVERY_TICKS == 0L) {
            inbox.addLast(MatchMsg.Progress(state.score, state.wellHeight))
        }
    }

    override fun send(msg: MatchMsg) {
        if (msg is MatchMsg.Garbage) SnakeDropEngine.receiveGarbage(state, msg.rows)
    }

    override fun poll(): MatchMsg? = inbox.removeFirstOrNull()

    private companion object {
        const val DEFAULT_RANDOM_ONE_IN = 12
        const val DEFAULT_DECIDE_EVERY = 7
        const val PROGRESS_EVERY_TICKS = 30L
        const val BOT_SALT = 0x5DEECE66DL
    }
}
