package com.snakedrop.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.snakedrop.engine.core.FixedStepAccumulator
import com.snakedrop.game.Dir
import com.snakedrop.game.GameConstants.SIM_HZ
import com.snakedrop.game.GameConstants.WIDTH
import com.snakedrop.game.Phase
import com.snakedrop.game.SnakeDropEngine
import com.snakedrop.platform.ScoreStore
import com.snakedrop.render.BOARD_ROWS
import com.snakedrop.render.Palette
import com.snakedrop.render.drawBoard
import kotlin.math.abs

private const val SWIPE_PX = 36f
private const val NANOS_PER_SEC = 1_000_000_000L
private const val NANOS_PER_MS = 1_000_000L

@Composable
fun GameScreen(
    store: ScoreStore,
    onHome: () -> Unit,
) {
    var run by remember { mutableIntStateOf(0) }
    val state = remember(run) { SnakeDropEngine.newRun(System.nanoTime()) }
    val acc = remember(run) { FixedStepAccumulator(NANOS_PER_SEC / SIM_HZ) }
    var frame by remember { mutableLongStateOf(0L) }
    var score by remember(run) { mutableIntStateOf(0) }
    var ended by remember(run) { mutableStateOf(false) }

    LaunchedEffect(run) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0L else now - last
                last = now
                if (state.phase == Phase.RUNNING) {
                    repeat(acc.consume(dt)) { SnakeDropEngine.step(state) }
                }
                state.events.clear()
                if (state.score != score) score = state.score
                if (state.phase == Phase.ENDED && !ended) {
                    ended = true
                    if (state.score > store.bestScore) store.bestScore = state.score
                }
                frame = now
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Palette.Ink).statusBarsPadding().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ArcadeText("SCORE $score", 14, Palette.Snake)
            Box(Modifier.clickable { togglePause(state) }) {
                ArcadeText(if (state.phase == Phase.PAUSED) "RESUME" else "PAUSE", 14, Palette.AccentAlt)
            }
        }
        Box(Modifier.fillMaxSize().padding(top = 8.dp)) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(run) {
                    var dx = 0f
                    var dy = 0f
                    detectDragGestures(
                        onDragStart = {
                            dx = 0f
                            dy = 0f
                        },
                    ) { change, drag ->
                        change.consume()
                        dx += drag.x
                        dy += drag.y
                        if (abs(dx) >= SWIPE_PX || abs(dy) >= SWIPE_PX) {
                            val dir =
                                if (abs(dx) > abs(dy)) {
                                    if (dx > 0) Dir.RIGHT else Dir.LEFT
                                } else {
                                    if (dy > 0) Dir.DOWN else Dir.UP
                                }
                            SnakeDropEngine.queueTurn(state, dir)
                            dx = 0f
                            dy = 0f
                        }
                    }
                },
            ) {
                val cell = minOf(size.width / WIDTH, size.height / BOARD_ROWS)
                val origin = Offset((size.width - cell * WIDTH) / 2f, 0f)
                frame.let { drawBoard(state, cell, origin, frame / NANOS_PER_MS) }
            }
            if (ended) {
                Column(
                    Modifier.fillMaxSize().background(Palette.Ink.copy(alpha = 0.85f)).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    ArcadeText("GAME OVER", 28, Palette.Apple)
                    ArcadeText("SCORE $score   BEST ${store.bestScore}", 14)
                    ArcadeButton("AGAIN") { run++ }
                    ArcadeButton("HOME", primary = false, onClick = onHome)
                }
            }
        }
    }
}

private fun togglePause(s: com.snakedrop.game.GameState) {
    if (s.phase == Phase.PAUSED) SnakeDropEngine.resume(s) else SnakeDropEngine.pause(s)
}
