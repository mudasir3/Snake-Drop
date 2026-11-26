package com.snakedrop.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snakedrop.render.Palette

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

/** Square-cornered button. Primary = lime fill; secondary = cyan outline. */
@Composable
internal fun ArcadeButton(
    label: String,
    primary: Boolean = true,
    onClick: () -> Unit,
) {
    val base = Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick)
    val styled = if (primary) base.background(Palette.Accent) else base.border(2.dp, Palette.AccentAlt)
    Box(styled, contentAlignment = Alignment.Center) {
        ArcadeText(label, 14, if (primary) Palette.Ink else Palette.AccentAlt)
    }
}

@Composable
fun HomeScreen(
    best: Int,
    onPlay: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Palette.Ink).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        ArcadeText("SNAKE", 40, Palette.Snake)
        ArcadeText("DROP", 40, Palette.Apple)
        ArcadeText("BEST  $best", 14, Palette.TextDim)
        ArcadeButton("PLAY", onClick = onPlay)
    }
}
