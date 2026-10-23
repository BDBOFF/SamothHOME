package com.samoth.home.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.samoth.home.AppEntry

private val TileShape = RoundedCornerShape(14.dp)

/** Halo lumineux autour d'une tuile sélectionnée, dans la couleur de l'appli. */
private fun DrawScope.halo(color: Color, radius: Dp) {
    val steps = 12
    val step = 2.2.dp.toPx()
    for (i in steps downTo 1) {
        val s = i * step
        drawRoundRect(
            color = color.copy(alpha = 0.045f),
            topLeft = Offset(-s, -s),
            size = Size(size.width + 2 * s, size.height + 2 * s),
            cornerRadius = CornerRadius(radius.toPx() + s),
        )
    }
}

/** Tuile en verre transparent, avec la vraie bannière (ou icône) de l'appli et un halo au focus. */
@Composable
fun AppTile(
    app: AppEntry,
    modifier: Modifier = Modifier,
    alwaysShowLabel: Boolean = false,
    focusModifier: Modifier = Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.1f else 1f, tween(180), label = "scale")

    Column(modifier.zIndex(if (focused) 1f else 0f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .drawBehind { if (focused) halo(app.color, 14.dp) }
                .clip(TileShape)
                .background(if (focused) app.color.copy(alpha = 0.22f) else Samoth.Glass)
                .border(
                    if (focused) 1.5.dp else 1.dp,
                    if (focused) app.color.copy(alpha = 0.8f) else Samoth.GlassLine,
                    TileShape,
                )
                .then(focusModifier)
                .onFocusChanged { if (it.isFocused) onFocused() }
                .clickable(interactionSource = source, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val a = if (focused) 1f else 0.85f
            when {
                app.banner != null -> Image(
                    app.banner, contentDescription = app.label,
                    modifier = Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(8.dp)).alpha(a),
                    contentScale = ContentScale.Fit,
                )
                app.icon != null -> Image(
                    app.icon, contentDescription = app.label,
                    modifier = Modifier.size(50.dp).alpha(a),
                )
                else -> Text(app.label.take(1), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(if (focused) 12.dp else 8.dp))
        Text(
            app.label,
            color = when {
                focused -> Samoth.Text
                alwaysShowLabel -> Samoth.Dim
                else -> Color.Transparent
            },
            fontFamily = Inter,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = if (focused) 11.sp else 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Bouton rond en verre (roue crantée, retour). */
@Composable
fun GlassCircleButton(icon: ImageVector, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, tween(160), label = "btn")
    Box(
        modifier
            .size(32.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(if (focused) Color.White.copy(alpha = 0.9f) else Samoth.Glass)
            .border(1.dp, if (focused) Color.White else Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (focused) Samoth.Bg else Color(0xFFE8E8ED), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SettingsButton(modifier: Modifier = Modifier, onClick: () -> Unit) =
    GlassCircleButton(Icons.Filled.Settings, "Réglages de la TV", modifier, onClick)

@Composable
fun BackButton(modifier: Modifier = Modifier, onClick: () -> Unit) =
    GlassCircleButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", modifier, onClick)

/** Pastille arrondie (Toutes les applis, mise à jour). */
@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    leading: (@Composable (Color) -> Unit)? = null,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(160), label = "pill")
    val bg = when {
        focused -> Color.White.copy(alpha = 0.92f)
        accent != null -> accent.copy(alpha = 0.85f)
        else -> Samoth.Glass
    }
    val fg = if (focused) Samoth.Bg else Samoth.Text
    Row(
        modifier
            .height(30.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(15.dp))
            .background(bg)
            .border(1.dp, if (focused) Color.White else Color.White.copy(alpha = 0.14f), RoundedCornerShape(15.dp))
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        leading?.invoke(fg)
        Text(text, color = fg, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
    }
}

/** Petite grille de 4 carrés (icône « Toutes les applis »). */
@Composable
fun GridGlyph(color: Color) {
    Canvas(Modifier.size(11.dp)) {
        val g = size.width * 0.18f
        val c = (size.width - g) / 2
        for (x in 0..1) for (y in 0..1) {
            drawRoundRect(
                color = color,
                topLeft = Offset(x * (c + g), y * (c + g)),
                size = Size(c, c),
                cornerRadius = CornerRadius(c * 0.25f),
            )
        }
    }
}
