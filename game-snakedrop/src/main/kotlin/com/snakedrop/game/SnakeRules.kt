@file:Suppress("ComplexCondition")

package com.snakedrop.game

import com.snakedrop.engine.core.IntRingQueue
import com.snakedrop.game.GameConstants.APPLES_PER_CHUNK
import com.snakedrop.game.GameConstants.APPLE_POINTS
import com.snakedrop.game.GameConstants.MOVE_THRESHOLD
import com.snakedrop.game.GameConstants.START_LEN
import com.snakedrop.game.GameConstants.WIDTH

/** Snake movement, turning, collisions, eating. */
internal object SnakeRules {
    /** Places the snake horizontally in the arena, head to the right, and spawns the first apple. */
    fun placeStartSnake(s: GameState) {
        val row = START_ROW
        for (i in 0 until START_LEN) {
            val cell = Grid.idx(row, START_COL + i)
            s.snake[i] = cell
            s.arena[cell] = if (i == START_LEN - 1) Cell.HEAD else Cell.BODY
        }
        s.snakeStart = 0
        s.snakeLen = START_LEN
        s.heading = Dir.RIGHT
    }

    /** Queues a turn if it is not a no-op and not a reversal of the heading it will follow. */
    fun queueTurn(
        s: GameState,
        dir: Int,
    ): Boolean {
        if (s.phase != Phase.RUNNING || dir !in Dir.UP..Dir.LEFT) return false
        val last = s.turns.peekLast()
        val projected = if (last == IntRingQueue.EMPTY) s.heading else last
        if (dir == projected || dir == Dir.opposite(projected)) return false
        return s.turns.offer(dir)
    }

    /** Adds speed and moves the snake when enough has accumulated. */
    fun tick(s: GameState) {
        s.moveAccum += s.snakeSpeed100
        if (s.moveAccum < MOVE_THRESHOLD) return
        s.moveAccum -= MOVE_THRESHOLD
        move(s)
    }

    private fun move(s: GameState) {
        val turn = s.turns.poll()
        if (turn != IntRingQueue.EMPTY) s.heading = turn
        val head = s.headIndex()
        val nr = Grid.row(head) + Dir.DROW[s.heading]
        val nc = Grid.col(head) + Dir.DCOL[s.heading]
        if (nr < 0 || nr >= s.arenaRows || nc < 0 || nc >= WIDTH) {
            RunControl.end(s, EndCause.WALL)
            return
        }
        val next = Grid.idx(nr, nc)
        val ate = next == s.appleIndex
        val tail = s.snakeCell(0)
        val hitsBody = s.arena[next] == Cell.BODY || s.arena[next] == Cell.HEAD
        if (hitsBody && (ate || next != tail)) {
            RunControl.end(s, EndCause.SELF)
            return
        }
        advance(s, head, next, ate)
        if (ate) onApple(s)
    }

    private fun advance(
        s: GameState,
        head: Int,
        next: Int,
        ate: Boolean,
    ) {
        if (!ate) {
            s.arena[s.snakeCell(0)] = Cell.EMPTY
            s.snakeStart = (s.snakeStart + 1) % s.snake.size
            s.snakeLen--
        }
        s.arena[head] = Cell.BODY
        s.snake[(s.snakeStart + s.snakeLen) % s.snake.size] = next
        s.snakeLen++
        s.arena[next] = Cell.HEAD
    }

    private fun onApple(s: GameState) {
        s.appleIndex = -1
        s.applesEaten++
        s.applesSinceDetach++
        s.score += APPLE_POINTS
        val c = s.config
        s.snakeSpeed100 = minOf(c.snakeSpeedMax100, c.snakeSpeedStart100 + c.snakeSpeedStep100 * s.applesEaten)
        s.events.add(Ev.APPLE_EATEN, s.applesEaten)
        if (s.applesSinceDetach >= APPLES_PER_CHUNK) ChunkRules.detachTail(s)
        if (s.phase == Phase.RUNNING) RunControl.spawnApple(s)
    }

    /** Picks a safe heading after a revive: keep the heading, else turn left or right; never reverse. */
    fun chooseSafeHeading(s: GameState) {
        val candidates = intArrayOf(s.heading, (s.heading + 3) % 4, (s.heading + 1) % 4)
        val head = s.headIndex()
        val tail = s.snakeCell(0)
        for (d in candidates) {
            val nr = Grid.row(head) + Dir.DROW[d]
            val nc = Grid.col(head) + Dir.DCOL[d]
            if (nr < 0 || nr >= s.arenaRows || nc < 0 || nc >= WIDTH) continue
            val cell = Grid.idx(nr, nc)
            val occupied = s.arena[cell] == Cell.BODY || s.arena[cell] == Cell.HEAD
            if (!occupied || cell == tail) {
                s.heading = d
                return
            }
        }
    }

    private const val START_ROW = 4
    private const val START_COL = 3
}
