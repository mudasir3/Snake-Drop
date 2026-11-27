@file:Suppress("MagicNumber", "MaxLineLength")

package com.snakedrop.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.snakedrop.game.Ev
import com.snakedrop.game.GameConstants.ARENA_ROWS
import com.snakedrop.game.GameConstants.CHUNK_LEN
import com.snakedrop.game.GameConstants.WIDTH
import com.snakedrop.game.Grid
import com.snakedrop.render.BOARD_ROWS
import com.snakedrop.render.Palette
import com.snakedrop.render.ParticleSystem
import kotlin.math.exp

/** Where the player's board was last drawn, so effects can be placed in pixels. Written by the canvas, read by the loop. */
internal class BoardMetrics {
    var cell = 0f
    var originX = 0f
    var originY = 0f
}

/** Screen shake, flash and particle state, fed by engine events. Plain fields: no allocation per frame. */
internal class Juice {
    val particles = ParticleSystem()
    var shake = 0f
    var flash = 0f
    var flashColor: Color = Palette.Snake
    private val lastChunkRow = IntArray(CHUNK_LEN)
    private val lastChunkCol = IntArray(CHUNK_LEN)
    private var haveChunk = false
    private var seed = 12345

    private fun rnd(): Float {
        seed = seed * 1103515245 + 12345
        return ((seed ushr 8) and 0xFFFF) / 65535f
    }

    /** Remembers the falling chunk's cells so a landing can burst where it was. */
    fun trackChunk(s: Session) {
        val st = s.state
        if (!st.chunkActive) return
        for (i in 0 until CHUNK_LEN) {
            lastChunkRow[i] = ARENA_ROWS + st.chunkTop + st.chunkRow[i]
            lastChunkCol[i] = st.chunkCol[i]
        }
        haveChunk = true
    }

    /** Turns the session's pending engine events into particles, shake, flash and haptics. */
    fun consume(
        s: Session,
        m: BoardMetrics,
        haptic: HapticFeedback,
    ) {
        if (m.cell <= 0f) {
            s.clearFx()
            return
        }
        for (i in 0 until s.fxCount) {
            when (s.fxType(i)) {
                Ev.APPLE_EATEN -> onApple(s, m, haptic)
                Ev.CHUNK_DETACHED -> {
                    shake = maxOf(shake, 5f)
                    burstChunk(m, Palette.Chunks[s.state.chunkColor.coerceIn(1, 6)], 6, 220f)
                }
                Ev.CHUNK_LANDED -> {
                    shake = maxOf(shake, 7f)
                    burstChunk(m, Palette.TextHi, 5, 160f, rowShift = 1)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                Ev.ROWS_CLEARED -> onRows(s.fxArg(i), m, haptic)
                Ev.GARBAGE_RECEIVED -> {
                    shake = maxOf(shake, 9f)
                    flash = 0.35f
                    flashColor = Palette.Danger
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                Ev.RUN_ENDED -> onEnd(s, m, haptic)
            }
        }
        s.clearFx()
    }

    private fun center(
        m: BoardMetrics,
        row: Int,
        col: Int,
        out: FloatArray,
    ) {
        out[0] = m.originX + (col + 0.5f) * m.cell
        out[1] = m.originY + (row + 0.5f) * m.cell
    }

    private val tmp = FloatArray(2)

    private fun onApple(
        s: Session,
        m: BoardMetrics,
        haptic: HapticFeedback,
    ) {
        val head = s.state.headIndex()
        center(m, Grid.row(head), Grid.col(head), tmp)
        particles.burst(tmp[0], tmp[1], 14, Palette.Apple, 260f, m.cell * 0.22f)
        particles.burst(tmp[0], tmp[1], 6, Palette.SnakeHead, 180f, m.cell * 0.18f)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    private fun burstChunk(
        m: BoardMetrics,
        color: Color,
        perCell: Int,
        speed: Float,
        rowShift: Int = 0,
    ) {
        if (!haveChunk) return
        for (i in 0 until CHUNK_LEN) {
            center(m, lastChunkRow[i] + rowShift, lastChunkCol[i], tmp)
            particles.burst(tmp[0], tmp[1], perCell, color, speed, m.cell * 0.2f, 0.45f)
        }
    }

    private fun onRows(
        rows: Int,
        m: BoardMetrics,
        haptic: HapticFeedback,
    ) {
        shake = maxOf(shake, 8f + rows * 4f)
        flash = 0.25f + rows * 0.1f
        flashColor = Palette.Snake
        for (c in 0 until WIDTH) {
            val row = BOARD_ROWS - 1 - (rnd() * 3).toInt()
            center(m, row, c, tmp)
            particles.burst(tmp[0], tmp[1], 4 + rows * 2, Palette.Chunks[1 + (c + rows) % 6], 380f + rows * 60f, m.cell * 0.24f, 0.8f)
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    private fun onEnd(
        s: Session,
        m: BoardMetrics,
        haptic: HapticFeedback,
    ) {
        shake = maxOf(shake, 14f)
        flash = 0.4f
        flashColor = Palette.Danger
        val head = s.state.headIndex()
        center(m, Grid.row(head), Grid.col(head), tmp)
        particles.burst(tmp[0], tmp[1], 40, Palette.Snake, 480f, m.cell * 0.26f, 1.0f)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** Decays shake and flash and moves particles. Call once per frame. */
    fun update(dtSec: Float) {
        val k = exp(-dtSec * 11f)
        shake *= k
        flash *= exp(-dtSec * 6f)
        if (shake < 0.1f) shake = 0f
        particles.update(dtSec)
    }

    /** Random shake offset component in pixels. */
    fun shakeOffset(): Float = (rnd() - 0.5f) * 2f * shake
}
