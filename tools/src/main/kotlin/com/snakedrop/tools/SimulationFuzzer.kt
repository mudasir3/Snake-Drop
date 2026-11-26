@file:Suppress("CyclomaticComplexMethod", "ComplexCondition", "LoopWithTooManyJumpStatements", "MagicNumber")

package com.snakedrop.tools

import com.snakedrop.engine.core.XorShift64
import com.snakedrop.game.Cell
import com.snakedrop.game.GameInvariants
import com.snakedrop.game.GameState
import com.snakedrop.game.Grid
import com.snakedrop.game.Phase
import com.snakedrop.game.SnakeDropEngine
import kotlin.system.exitProcess

private const val DEFAULT_RUNS = 10_000
private const val MAX_TICKS = 200_000
private const val CHECK_EVERY = 16
private const val DECIDE_EVERY = 7
private const val RANDOM_TURN_ONE_IN = 9
private const val REVIVE_ONE_IN = 3

private val DR = intArrayOf(-1, 0, 1, 0)
private val DC = intArrayOf(0, 1, 0, -1)

/** Greedy bot: step toward the apple unless that cell is blocked; sometimes turn randomly. */
private fun decide(
    s: GameState,
    rng: XorShift64,
) {
    val head = s.headIndex()
    val hr = Grid.row(head)
    val hc = Grid.col(head)
    val ar = if (s.appleIndex >= 0) Grid.row(s.appleIndex) else hr
    val ac = if (s.appleIndex >= 0) Grid.col(s.appleIndex) else hc
    var best = -1
    var bestDist = Int.MAX_VALUE
    for (d in 0 until 4) {
        if (d == (s.heading + 2) % 4) continue
        val r = hr + DR[d]
        val c = hc + DC[d]
        if (r < 0 || r >= s.arenaRows || c < 0 || c >= 12) continue
        val cell = s.arena[Grid.idx(r, c)]
        if (cell != Cell.EMPTY && cell != Cell.APPLE) continue
        val dist = Math.abs(r - ar) + Math.abs(c - ac)
        if (dist < bestDist) {
            bestDist = dist
            best = d
        }
    }
    if (rng.nextInt(RANDOM_TURN_ONE_IN) == 0) best = rng.nextInt(4)
    if (best >= 0 && best != s.heading) SnakeDropEngine.queueTurn(s, best)
}

private fun fail(
    seed: Long,
    tick: Int,
    msg: String,
): Nothing {
    System.err.println("FUZZ FAIL seed=$seed tick=$tick: $msg")
    exitProcess(1)
}

private fun runOne(seed: Long): Int {
    val s = SnakeDropEngine.newRun(seed)
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
        if (t % DECIDE_EVERY == 0) decide(s, rng)
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
