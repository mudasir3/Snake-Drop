@file:Suppress("MagicNumber", "MaxLineLength")

package com.snakedrop.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snakedrop.render.ArcadeTheme
import com.snakedrop.render.Palette
import com.snakedrop.render.drawBackdrop
import kotlinx.coroutines.delay

internal val Display = FontFamily.Monospace

@Composable
internal fun ArcadeText(
    text: String,
    size: Int,
    color: Color = Palette.TextHi,
) {
    val style = TextStyle(color = color, fontSize = size.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
    BasicText(text, style = style)
}

/** Milliseconds since first composition, updated every frame. Read it inside draw/graphicsLayer lambdas where possible. */
@Composable
internal fun rememberFrameMs(): State<Long> {
    val t = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t.longValue = (it - start) / 1_000_000L }
    }
    return t
}

/** Full-screen animated backdrop; colors ease to [theme] when it changes. */
@Composable
internal fun AppBackdrop(theme: ArcadeTheme) {
    val spec = tween<Color>(500)
    val top by animateColorAsState(theme.bandTop, spec, label = "top")
    val bottom by animateColorAsState(theme.bandBottom, spec, label = "bottom")
    val accent by animateColorAsState(theme.accent, spec, label = "accent")
    val t = rememberFrameMs()
    Canvas(Modifier.fillMaxSize()) { drawBackdrop(ArcadeTheme(top, bottom, accent), t.value) }
}

/** Hard offset-shadow button that presses in, with haptic feedback. Primary = lime; secondary = cyan outline. */
@Composable
internal fun ArcadeButton(
    label: String,
    primary: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(70), label = "press")
    val haptic = LocalHapticFeedback.current
    val shadowColor = if (primary) Color(0xFF5E7F1B) else Color(0xFF0E5E70)
    val depth = 5.dp
    Box(
        Modifier.fillMaxWidth().height(60.dp).drawBehind {
            val d = depth.toPx() * (1f - press)
            drawRect(shadowColor, Offset(d, d), Size(size.width - depth.toPx(), size.height - depth.toPx()))
        },
    ) {
        val face = if (primary) Modifier.background(Palette.Accent) else Modifier.background(Palette.Panel).border(2.dp, Palette.AccentAlt)
        Box(
            Modifier
                .fillMaxSize()
                .padding(end = depth, bottom = depth)
                .graphicsLayer {
                    translationX = depth.toPx() * press
                    translationY = depth.toPx() * press
                }.then(face)
                .clickable(interactionSource = interaction, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            ArcadeText(label, 14, if (primary) Palette.Ink else Palette.AccentAlt)
        }
    }
}

/** A pixel star (plus shape). [fill] 0..1 scales it in; unearned stars show as a dim outline. */
@Composable
internal fun PixelStar(
    earned: Boolean,
    fill: Float,
    sizeDp: Dp,
) {
    Canvas(Modifier.size(sizeDp)) {
        val u = size.width / 5f
        val cells = listOf(2 to 0, 1 to 1, 2 to 1, 3 to 1, 0 to 2, 1 to 2, 2 to 2, 3 to 2, 4 to 2, 1 to 3, 2 to 3, 3 to 3, 2 to 4)
        val s = if (earned) fill else 1f
        val color = if (earned) Palette.Snake else Palette.Grid
        for ((c, r) in cells) drawStarCell(color, c, r, u, s)
    }
}

private fun DrawScope.drawStarCell(
    color: Color,
    c: Int,
    r: Int,
    u: Float,
    scale: Float,
) {
    val inset = u * (1f - scale) / 2f
    drawRect(color, Offset(c * u + inset, r * u + inset), Size(u * scale, u * scale))
}

/** Row of three stars that pop in one after another. */
@Composable
internal fun StarsRow(
    count: Int,
    sizeDp: Dp = 36.dp,
    animate: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until 3) {
            val a = remember(count) { Animatable(if (animate) 0f else 1f) }
            LaunchedEffect(count) {
                if (animate && i < count) {
                    delay(300L + 260L * i)
                    a.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
            }
            PixelStar(i < count, if (i < count) a.value else 1f, sizeDp)
        }
    }
}

/** Counts up to [target] over about 700 ms. */
@Composable
internal fun rememberCountUp(target: Int): Int {
    val a = remember(target) { Animatable(0f) }
    LaunchedEffect(target) { a.animateTo(target.toFloat(), tween(700)) }
    return a.value.toInt()
}

/** Title letters that bob on a sine wave. */
@Composable
internal fun BobbingTitle(
    text: String,
    size: Int,
    color: Color,
    phase: Int,
) {
    val t = rememberFrameMs()
    Row {
        text.forEachIndexed { i, ch ->
            Box(Modifier.graphicsLayer { translationY = kotlin.math.sin(t.value / 280f + (i + phase) * 0.9f) * 9f }) {
                ArcadeText(ch.toString(), size, color)
            }
        }
    }
}

/** A little snake that crawls along the bottom of the home screen toward an apple, then repeats. */
@Composable
internal fun CrawlingSnake(modifier: Modifier = Modifier) {
    val t = rememberFrameMs()
    Canvas(modifier.fillMaxWidth().height(48.dp)) {
        val u = size.height / 2f
        val period = 9000L
        val p = (t.value % period).toFloat() / period
        val travel = size.width + u * 14f
        val headX = -u + p * travel
        val appleX = size.width - u * 3f
        if (headX < appleX) drawRect(Palette.Apple, Offset(appleX, u / 2f), Size(u, u))
        for (i in 0 until 6) {
            val x = headX - i * u * 1.1f
            drawRect(if (i == 0) Palette.SnakeHead else Palette.Snake, Offset(x, u / 2f), Size(u * 0.9f, u))
        }
    }
}
