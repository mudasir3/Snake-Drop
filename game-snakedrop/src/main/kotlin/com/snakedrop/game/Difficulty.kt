@file:Suppress("MagicNumber")

package com.snakedrop.game

/** Difficulty presets: speed, chunk fall, arena shrink and starting rubble. MEDIUM is the PRD baseline. */
public enum class Difficulty(public val config: GameConfig) {
    SMALL(
        GameConfig(
            snakeSpeedStart100 = 500,
            snakeSpeedStep100 = 20,
            snakeSpeedMax100 = 900,
            chunkFallMs = 190,
            chunkFallMsFast = 150,
            arenaShrinkThreshold = 7,
            shrinkWarningMs = 2000,
        ),
    ),
    MEDIUM(GameConfig()),
    HARD(
        GameConfig(
            snakeSpeedStart100 = 700,
            snakeSpeedStep100 = 30,
            snakeSpeedMax100 = 1300,
            chunkFallMs = 120,
            chunkFallMsFast = 90,
            arenaShrinkThreshold = 5,
            shrinkWarningMs = 1100,
            startRubbleRows = 2,
        ),
    ),
}
