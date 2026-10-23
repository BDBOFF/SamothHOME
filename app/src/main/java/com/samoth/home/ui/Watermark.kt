package com.samoth.home.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val BaseStyle = TextStyle(fontFamily = Unbounded, letterSpacing = (-0.02).em)

/**
 * Grand nom en fond, très gras et transparent : « SAMOTH HOME » à l'allumage,
 * puis le nom de l'appli sélectionnée, dans sa couleur.
 */
@Composable
fun Watermark(text: String, color: Color, height: Dp, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(color.copy(alpha = 0.34f), tween(450), label = "mark")
    BoxWithConstraints(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val maxW = constraints.maxWidth
        val maxH = with(density) { height.toPx() }
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
            label = "markText",
        ) { value ->
            val size: TextUnit = remember(value, maxW) {
                val probe = 100.sp
                val w = measurer.measure(value, BaseStyle.copy(fontSize = probe), softWrap = false).size.width
                val byWidth = if (w > 0) 100f * maxW / w else 100f
                val byHeight = 100f * maxH / with(density) { (probe * 1.25f).toPx() }
                minOf(byWidth * 0.98f, byHeight, 118f).sp
            }
            Text(
                value,
                style = BaseStyle.copy(fontSize = size, lineHeight = size, color = tint),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
