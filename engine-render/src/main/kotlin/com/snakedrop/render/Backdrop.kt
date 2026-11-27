@file:Suppress("MagicNumber")

package com.snakedrop.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

private const val GHOSTS = 22
private const val BANDS = 6
private const val SCAN_STEP = 6f

/** Tiny deterministic hash so ghost pieces keep stable positions across frames without any allocation. */
private fun hash(n: Int): Float {
    var x = n * 374761393 + 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}

/**
 * Retro backdrop: stepped flat color bands, three parallax layers of dim falling 4-cell pieces (the game's own
 * motif), and faint scanlines. Hard edges only, in keeping with the board.
 */
fun DrawScope.drawBackdrop(
    theme: ArcadeTheme,
    timeMs: Long,
) {
    drawRect(theme.bandBottom, Offset.Zero, size)
    val bandH = size.height / BANDS
    for (i in 0 until BANDS / 2) {
        val a = 1f - i / (BANDS / 2f)
        drawRect(theme.bandTop.copy(alpha = a * 0.55f), Offset(0f, i * bandH), Size(size.width, bandH))
    }
    val t = timeMs / 1000f
    for (i in 0 until GHOSTS) {
        val layer = i % 3
        val unit = size.width / (14f + layer * 4f)
        val speed = (8f + layer * 9f) * unit / 24f
        val x = hash(i * 7 + 1) * (size.width - unit * 4f)
        val span = size.height + unit * 6f
        val y = (hash(i * 13 + 5) * span + t * speed) % span - unit * 3f
        val shape = (hash(i * 3 + 9) * 4f).toInt()
        val alpha = 0.05f + layer * 0.035f
        val c = theme.accent.copy(alpha = alpha)
        for (k in 0 until 4) {
            val (dx, dy) = ghostCell(shape, k)
            drawRect(c, Offset(x + dx * unit, y + dy * unit), Size(unit * 0.86f, unit * 0.86f))
        }
    }
    var y = 0f
    while (y < size.height) {
        drawRect(theme.bandBottom.copy(alpha = 0.18f), Offset(0f, y), Size(size.width, 1.5f))
        y += SCAN_STEP
    }
}

private fun ghostCell(
    shape: Int,
    k: Int,
): Pair<Int, Int> =
    when (shape) {
        0 -> k to 0
        1 -> (if (k < 3) k else 1) to (if (k < 3) 0 else 1)
        2 -> (k % 2) to (k / 2)
        else -> (if (k < 2) k else k - 1) to (if (k < 2) 0 else 1)
    }
