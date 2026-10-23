package com.samoth.home.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.samoth.home.R

object Samoth {
    val Bg = Color(0xFF07070A)
    val Red = Color(0xFFD10A20)
    val Text = Color(0xFFF5F5F7)
    val Muted = Color(0xFFA3A3B2)
    val Dim = Color(0xFF8E8E93)
    val Glass = Color.White.copy(alpha = 0.06f)
    val GlassLine = Color.White.copy(alpha = 0.12f)
}

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) =
    Font(R.font.inter, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

@OptIn(ExperimentalTextApi::class)
val Unbounded = FontFamily(
    Font(R.font.unbounded, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900)))
)

val Inter = FontFamily(inter(300), inter(400), inter(500), inter(600), inter(700))

@Composable
fun SamothTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Samoth.Red,
            background = Samoth.Bg,
            surface = Samoth.Bg,
            onBackground = Samoth.Text,
            onSurface = Samoth.Text,
        ),
        content = content,
    )
}
