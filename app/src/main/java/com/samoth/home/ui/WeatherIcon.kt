package com.samoth.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.samoth.home.Sky
import kotlin.math.cos
import kotlin.math.sin

private val SunColor = Color(0xFFFFC24B)
private val MoonColor = Color(0xFFE8E8ED)
private val CloudColor = Color(0xFFE8E8ED)
private val RainColor = Color(0xFF5AC8FF)
private val BoltColor = Color(0xFFFFC24B)

/** Petite icône météo dessinée (soleil, lune, nuage, pluie, orage, neige, brouillard). */
@Composable
fun WeatherIcon(sky: Sky, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        when (sky) {
            Sky.Clear -> if (isDay) sun(Offset(w * 0.5f, w * 0.5f), w * 0.2f) else moon(Offset(w * 0.5f, w * 0.5f), w * 0.3f)
            Sky.PartlyCloudy -> {
                if (isDay) sun(Offset(w * 0.36f, w * 0.36f), w * 0.15f) else moon(Offset(w * 0.38f, w * 0.36f), w * 0.22f)
                cloud(Offset(w * 0.08f, w * 0.42f), w * 0.86f)
            }
            Sky.Cloudy -> cloud(Offset(w * 0.04f, w * 0.26f), w * 0.92f)
            Sky.Fog -> {
                val s = Stroke(w * 0.08f, cap = StrokeCap.Round)
                for (i in 0..2) {
                    val y = w * (0.3f + i * 0.2f)
                    drawLine(CloudColor.copy(alpha = 0.85f), Offset(w * (0.12f + i * 0.06f), y), Offset(w * (0.88f - i * 0.04f), y), s.width, StrokeCap.Round)
                }
            }
            Sky.Rain -> {
                cloud(Offset(w * 0.04f, w * 0.1f), w * 0.92f)
                for (i in 0..2) {
                    val x = w * (0.3f + i * 0.2f)
                    drawLine(RainColor, Offset(x, w * 0.68f), Offset(x - w * 0.07f, w * 0.92f), w * 0.07f, StrokeCap.Round)
                }
            }
            Sky.Storm -> {
                cloud(Offset(w * 0.04f, w * 0.1f), w * 0.92f)
                val p = Path().apply {
                    moveTo(w * 0.55f, w * 0.6f); lineTo(w * 0.4f, w * 0.8f); lineTo(w * 0.52f, w * 0.8f); lineTo(w * 0.44f, w * 0.98f)
                }
                drawPath(p, BoltColor, style = Stroke(w * 0.07f, cap = StrokeCap.Round))
            }
            Sky.Snow -> {
                cloud(Offset(w * 0.04f, w * 0.1f), w * 0.92f)
                for (i in 0..2) drawCircle(Color.White, w * 0.05f, Offset(w * (0.3f + i * 0.2f), w * (0.8f + (i % 2) * 0.1f)))
            }
        }
    }
}

private fun DrawScope.sun(c: Offset, r: Float) {
    drawCircle(SunColor, r, c)
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val x1 = c.x + cos(a).toFloat() * r * 1.5f
        val y1 = c.y + sin(a).toFloat() * r * 1.5f
        val x2 = c.x + cos(a).toFloat() * r * 2.1f
        val y2 = c.y + sin(a).toFloat() * r * 2.1f
        drawLine(SunColor, Offset(x1, y1), Offset(x2, y2), r * 0.35f, StrokeCap.Round)
    }
}

private fun DrawScope.moon(c: Offset, r: Float) {
    val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
    val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + r * 0.45f, c.y - r * 0.3f), r * 0.85f)) }
    val crescent = Path().apply { op(outer, cut, androidx.compose.ui.graphics.PathOperation.Difference) }
    drawPath(crescent, MoonColor)
}

/** Nuage : trois bosses et une base arrondie, dans un carré de côté [w] à partir de [o]. */
private fun DrawScope.cloud(o: Offset, w: Float) {
    val h = w * 0.55f
    drawRoundRect(
        CloudColor, topLeft = Offset(o.x, o.y + h * 0.45f), size = Size(w, h * 0.55f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.28f),
    )
    drawCircle(CloudColor, h * 0.32f, Offset(o.x + w * 0.3f, o.y + h * 0.5f))
    drawCircle(CloudColor, h * 0.42f, Offset(o.x + w * 0.58f, o.y + h * 0.42f))
}
