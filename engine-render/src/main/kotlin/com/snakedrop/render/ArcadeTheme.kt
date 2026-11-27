package com.snakedrop.render

import androidx.compose.ui.graphics.Color
import com.snakedrop.game.Difficulty

/** Per-difficulty look: two flat backdrop bands plus the accent used for ambient pixels and glows. */
class ArcadeTheme(
    val bandTop: Color,
    val bandBottom: Color,
    val accent: Color,
) {
    companion object {
        /** Small: cool teal. */
        val Calm = ArcadeTheme(Color(0xFF0C2A33), Color(0xFF061218), Color(0xFF25D9FF))

        /** Medium and menus: midnight violet. */
        val Midnight = ArcadeTheme(Color(0xFF1A1440), Color(0xFF0A0820), Color(0xFFA66BFF))

        /** Hard: ember red. */
        val Ember = ArcadeTheme(Color(0xFF3A0F14), Color(0xFF140508), Color(0xFFFF6B3D))

        /** Theme for a difficulty. */
        fun of(difficulty: Difficulty): ArcadeTheme =
            when (difficulty) {
                Difficulty.SMALL -> Calm
                Difficulty.MEDIUM -> Midnight
                Difficulty.HARD -> Ember
            }
    }
}
