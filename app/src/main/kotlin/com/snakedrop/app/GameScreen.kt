@file:Suppress("MaxLineLength", "MagicNumber", "LongParameterList")

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.snakedrop.engine.core.FixedStepAccumulator
import com.snakedrop.game.Dir
import com.snakedrop.game.GameConstants.SIM_HZ
import com.snakedrop.game.GameConstants.WIDTH
import com.snakedrop.game.Levels
import com.snakedrop.render.BOARD_ROWS
import com.snakedrop.render.Palette
import com.snakedrop.render.drawBoard
import kotlin.math.abs

private const val SWIPE_PX = 36f
private const val NANOS_PER_SEC = 1_000_000_000L
private const val NANOS_PER_MS = 1_000_000L
private const val OPPONENT_SHARE = 0.34f

/** Hooks the host screen needs when a session finishes. */
class SessionResult(
    val outcome: Outcome,
    val score: Int,
    val stars: Int,
    val session: Session,
)

@Composable
fun GameScreen(
    session: Session,
    onFinished: (SessionResult) -> Unit,
    onQuit: () -> Unit,
) {
    val acc = remember(session) { FixedStepAccumulator(NANOS_PER_SEC / SIM_HZ) }
    val juice = remember(session) { Juice() }
    val metrics = remember(session) { BoardMetrics() }
    val haptic = LocalHapticFeedback.current
    var frame by remember { mutableLongStateOf(0L) }
    var score by remember(session) { mutableIntStateOf(0) }
    var goal by remember(session) { mutableIntStateOf(0) }
    var paused by remember(session) { mutableStateOf(false) }

    LaunchedEffect(session) {
        var last = 0L
        var reported = false
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0L else now - last
                last = now
                if (session.outcome() == Outcome.PLAYING && !paused) repeat(acc.consume(dt)) { session.step() }
                juice.consume(session, metrics, haptic)
                juice.trackChunk(session)
                juice.update(dt / NANOS_PER_SEC.toFloat())
                val s = session.state
                if (s.score != score) score = s.score
                val m = session.mode
                if (m is GameMode.LevelRun) goal = Levels.progress(s, m.level)
                frame = now
                val outcome = session.outcome()
                if (outcome != Outcome.PLAYING && !reported) {
                    reported = true
                    val stars = (m as? GameMode.LevelRun)?.let { if (outcome == Outcome.LEVEL_WON) Levels.stars(s, it.level) else 0 } ?: 0
                    onFinished(SessionResult(outcome, s.score, stars, session))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp)) {
        Hud(session, score, goal, paused, onPause = { paused = !paused }, onQuit = onQuit)
        Box(Modifier.fillMaxSize().padding(top = 8.dp)) {
            Column(Modifier.fillMaxSize()) {
                if (session.bot != null) {
                    Canvas(Modifier.fillMaxWidth().weight(OPPONENT_SHARE)) {
                        val cell = minOf(size.width / WIDTH, size.height / BOARD_ROWS)
                        drawBoard(session.bot.state, cell, Offset((size.width - cell * WIDTH) / 2f, 0f), frame / NANOS_PER_MS)
                    }
                    Box(Modifier.height(6.dp))
                }
                Canvas(
                    Modifier.fillMaxWidth().weight(1f).pointerInput(session) { swipeInput(session) },
                ) {
                    val cell = minOf(size.width / WIDTH, size.height / BOARD_ROWS)
                    val origin = Offset((size.width - cell * WIDTH) / 2f, 0f)
                    metrics.cell = cell
                    metrics.originX = origin.x
                    metrics.originY = origin.y
                    val nowMs = frame / NANOS_PER_MS
                    translate(juice.shakeOffset(), juice.shakeOffset()) {
                        drawBoard(session.state, cell, origin, nowMs)
                        juice.particles.draw(this)
                    }
                    drawDangerVignette(session.state.shrinkWarnUs > 0, nowMs)
                    if (juice.flash > 0.01f) drawRect(juice.flashColor.copy(alpha = juice.flash.coerceAtMost(0.5f)))
                }
            }
            if (paused) {
                Column(
                    Modifier.fillMaxSize().background(Palette.Ink.copy(alpha = 0.85f)).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    ArcadeText("PAUSED", 28, Palette.AccentAlt)
                    ArcadeButton("RESUME") { paused = false }
                    ArcadeButton("QUIT", primary = false, onClick = onQuit)
                }
            }
        }
    }
}

@Composable
private fun Hud(
    session: Session,
    score: Int,
    goal: Int,
    paused: Boolean,
    onPause: () -> Unit,
    onQuit: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            ArcadeText("SCORE $score", 14, Palette.Snake)
            when (val m = session.mode) {
                is GameMode.LevelRun -> ArcadeText("${m.level.goalType.name} $goal/${m.level.target}", 11, Palette.AccentAlt)
                is GameMode.Versus -> ArcadeText("BOT ${session.opponentScore}", 11, Palette.Apple)
                else -> Unit
            }
        }
        if (session.isLive) {
            Box(Modifier.clickable(onClick = onQuit).padding(6.dp)) { ArcadeText("QUIT", 14, Palette.TextDim) }
        } else {
            Box(Modifier.clickable(onClick = onPause).padding(6.dp)) {
                ArcadeText(if (paused) "RESUME" else "PAUSE", 14, Palette.AccentAlt)
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.swipeInput(session: Session) {
    var dx = 0f
    var dy = 0f
    detectDragGestures(onDragStart = {
        dx = 0f
        dy = 0f
    }) { change, drag ->
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
            session.turn(dir)
            dx = 0f
            dy = 0f
        }
    }
}

/** Pulsing red frame while the arena is about to shrink. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDangerVignette(
    active: Boolean,
    nowMs: Long,
) {
    if (!active) return
    val a = 0.18f + 0.2f * (0.5f + 0.5f * kotlin.math.sin(nowMs / 110f))
    val c = Palette.Danger.copy(alpha = a)
    val t = 16f
    drawRect(c, Offset.Zero, androidx.compose.ui.geometry.Size(size.width, t))
    drawRect(c, Offset(0f, size.height - t), androidx.compose.ui.geometry.Size(size.width, t))
    drawRect(c, Offset.Zero, androidx.compose.ui.geometry.Size(t, size.height))
    drawRect(c, Offset(size.width - t, 0f), androidx.compose.ui.geometry.Size(t, size.height))
}
