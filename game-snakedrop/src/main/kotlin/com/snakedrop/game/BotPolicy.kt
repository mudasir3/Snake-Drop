@file:Suppress("ComplexCondition", "LoopWithTooManyJumpStatements")

package com.snakedrop.game

import com.snakedrop.engine.core.XorShift64

/**
 * Greedy autoplayer used for the versus bot, the fuzzer and level-solvability tests: heads for the apple,
 * never into a wall or body, with an optional random turn to make it fallible.
 */
public object BotPolicy {
    /**
     * Picks a heading for [s] and passes it to [turn] when it differs from the current one.
     * [randomTurnOneIn] > 0 makes the bot pick a random direction 1 time in that many decisions.
     */
    public fun decide(
        s: GameState,
        rng: XorShift64,
        randomTurnOneIn: Int,
        turn: (Int) -> Unit,
    ) {
        var best = bestHeading(s)
        if (randomTurnOneIn > 0 && rng.nextInt(randomTurnOneIn) == 0) best = rng.nextInt(DIRS)
        if (best >= 0 && best != s.heading) turn(best)
    }

    private fun bestHeading(s: GameState): Int {
        val head = s.headIndex()
        val hr = Grid.row(head)
        val hc = Grid.col(head)
        val ar = if (s.appleIndex >= 0) Grid.row(s.appleIndex) else hr
        val ac = if (s.appleIndex >= 0) Grid.col(s.appleIndex) else hc
        var best = -1
        var bestDist = Int.MAX_VALUE
        for (d in 0 until DIRS) {
            if (d == Dir.opposite(s.heading)) continue
            val r = hr + Dir.DROW[d]
            val c = hc + Dir.DCOL[d]
            if (!isFree(s, r, c)) continue
            val dist = Math.abs(r - ar) + Math.abs(c - ac)
            if (dist < bestDist) {
                bestDist = dist
                best = d
            }
        }
        return best
    }

    private fun isFree(
        s: GameState,
        r: Int,
        c: Int,
    ): Boolean {
        if (r < 0 || r >= s.arenaRows || c < 0 || c >= GameConstants.WIDTH) return false
        val cell = s.arena[Grid.idx(r, c)]
        return cell == Cell.EMPTY || cell == Cell.APPLE
    }

    private const val DIRS = 4
}
