@file:Suppress("CyclomaticComplexMethod", "ComplexCondition", "LoopWithTooManyJumpStatements", "MagicNumber")

package com.snakedrop.tools

import com.snakedrop.engine.core.XorShift64
import com.snakedrop.game.BotPolicy
import com.snakedrop.game.Difficulty
import com.snakedrop.game.GameInvariants
import com.snakedrop.game.Phase
import com.snakedrop.game.RunSetup
import com.snakedrop.game.SnakeDropEngine
import kotlin.system.exitProcess

private const val DEFAULT_RUNS = 10_000
private const val MAX_TICKS = 200_000
private const val CHECK_EVERY = 16
private const val DECIDE_EVERY = 7
private const val GARBAGE_EVERY = 900
private const val RANDOM_TURN_ONE_IN = 9
private const val REVIVE_ONE_IN = 3

private fun fail(
    seed: Long,
    tick: Int,
    msg: String,
): Nothing {
    System.err.println("FUZZ FAIL seed=$seed tick=$tick: $msg")
    exitProcess(1)
}

private fun runOne(seed: Long): Int {
    val s = RunSetup(seed, Difficulty.entries[(seed % Difficulty.entries.size).toInt()]).create()
    val rng = XorShift64(seed xor 0x5DEECE66DL)
    var revived = false
    var lastScore = 0
    var t = 0
    while (t < MAX_TICKS) {
        if (s.phase == Phase.ENDED) {
            if (!revived && SnakeDropEngine.canRevive(s) && rng.nextInt(REVIVE_ONE_IN) == 0) {
                revived = true
                SnakeDropEngine.revive(s)
            } else {
                break
            }
        }
        if (t % GARBAGE_EVERY == 0 && rng.nextInt(2) == 0) SnakeDropEngine.receiveGarbage(s, 1 + rng.nextInt(3))
        if (t % DECIDE_EVERY == 0) BotPolicy.decide(s, rng, RANDOM_TURN_ONE_IN) { SnakeDropEngine.queueTurn(s, it) }
        SnakeDropEngine.step(s)
        val eventTick = s.events.count > 0
        if (s.events.dropped != 0) fail(seed, t, "events dropped")
        s.events.clear()
        if (s.score < lastScore) fail(seed, t, "score decreased")
        lastScore = s.score
        if (eventTick || t % CHECK_EVERY == 0) {
            val err = GameInvariants.check(s)
            if (err != null) fail(seed, t, err)
        }
        t++
    }
    val err = GameInvariants.check(s)
    if (err != null) fail(seed, t, err)
    return s.score
}

/** Entry point: `runFuzzer [runs]`. Exits non-zero on the first broken invariant. */
public fun main(args: Array<String>) {
    val runs = args.firstOrNull()?.toIntOrNull() ?: DEFAULT_RUNS
    var best = 0
    var total = 0L
    for (i in 1..runs) {
        val sc = runOne(i.toLong())
        total += sc
        if (sc > best) best = sc
    }
    println("fuzz ok: runs=$runs avgScore=${total / runs} bestScore=$best")
}
