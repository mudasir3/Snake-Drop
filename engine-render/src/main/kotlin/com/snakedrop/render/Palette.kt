package com.snakedrop.render

import androidx.compose.ui.graphics.Color

/** Midnight-arcade palette from docs/Design.md. */
object Palette {
    val Ink = Color(0xFF0B0F1A)
    val Grid = Color(0xFF1A2238)
    val Panel = Color(0xFF121829)
    val Snake = Color(0xFFB6FF3B)
    val SnakeHead = Color(0xFFE4FF8A)
    val Apple = Color(0xFFFF2E4D)
    val Danger = Color(0xFFFF3B30)
    val TextHi = Color(0xFFE8F0FF)
    val TextDim = Color(0xFF7C8AA8)
    val Accent = Color(0xFFB6FF3B)
    val AccentAlt = Color(0xFF25D9FF)

    /** Chunk colors, indexed by color id 1..6 (index 0 unused). */
    val Chunks =
        arrayOf(
            Color.Transparent,
            Color(0xFFFF3D81),
            Color(0xFFFFB020),
            Color(0xFF25D9FF),
            Color(0xFFA66BFF),
            Color(0xFF3DFF9A),
            Color(0xFFFF6B3D),
        )
}
