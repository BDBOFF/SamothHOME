package com.samoth.home.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.samoth.home.AppEntry
import com.samoth.home.HomeViewModel

private val PanelShape = RoundedCornerShape(22.dp)

/**
 * Le menu s'ouvre alors que OK est encore enfoncé (appui long) : on ignore cette touche
 * tant qu'elle n'a pas été relâchée puis appuyée à nouveau, sinon le relâchement
 * « cliquerait » le premier bouton du menu.
 */
@Composable
private fun Panel(width: Int, content: @Composable () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    Box(
        Modifier
            .onPreviewKeyEvent { e ->
                val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
                if (!ok || armed) return@onPreviewKeyEvent false
                if (e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 0) {
                    armed = true
                    false
                } else true // répétitions et relâchement de l'appui long : ignorés
            }
            .width(width.dp)
            .clip(PanelShape)
            .background(Color(0xF2141418))
            .border(1.dp, Color.White.copy(alpha = 0.12f), PanelShape)
            .padding(24.dp)
    ) { content() }
}

/** Bouton pleine largeur d'un menu. */
@Composable
fun MenuButton(
    text: String,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.03f else 1f, tween(140), label = "menu")
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.06f))
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leading?.invoke()
        Text(
            text,
            color = when {
                focused -> Samoth.Bg
                danger -> Color(0xFFFF6B6B)
                else -> Samoth.Text
            },
            fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
        )
    }
}

@Composable
private fun AppThumb(app: AppEntry) {
    val m = Modifier.size(width = 36.dp, height = 20.dp).clip(RoundedCornerShape(5.dp))
    when {
        app.banner != null -> Image(app.banner, null, m, contentScale = ContentScale.Crop)
        app.icon != null -> Image(app.icon, null, Modifier.size(20.dp))
        else -> Spacer(m.background(app.color))
    }
}

/** Menu de l'appui long : ajouter / remplacer / déplacer / retirer. */
@Composable
fun TileMenuDialog(vm: HomeViewModel) {
    val menu = vm.menu ?: return
    val app = menu.app
    val first = remember(menu) { FocusRequester() }
    Dialog(onDismissRequest = { vm.closeMenu() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(width = 340) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppThumb(app)
                    Spacer(Modifier.width(10.dp))
                    Text(app.label, color = Samoth.Text, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
                when {
                    menu.choosingSlot -> {
                        Text("Remplacer quelle appli de l'accueil ?", color = Samoth.Muted, fontFamily = Inter, fontSize = 12.sp)
                        vm.favorites.forEachIndexed { i, fav ->
                            MenuButton(
                                fav.label,
                                modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                                leading = { AppThumb(fav) },
                            ) { vm.replaceSlot(i, app) }
                        }
                        MenuButton("Annuler") { vm.closeMenu() }
                    }
                    menu.fromHome -> {
                        val pos = vm.positionOf(app)
                        val last = vm.favorites.size - 1
                        // Le premier bouton reçoit le focus ; toujours présent
                        if (pos > 0) MenuButton("◀  Déplacer à gauche", Modifier.focusRequester(first)) { vm.move(app, -1) }
                        if (pos in 0 until last) MenuButton("Déplacer à droite  ▶", if (pos == 0) Modifier.focusRequester(first) else Modifier) { vm.move(app, +1) }
                        MenuButton("Retirer de l'accueil", if (last <= 0) Modifier.focusRequester(first) else Modifier, danger = true) { vm.removeFromHome(app) }
                        MenuButton("Ouvrir") { vm.closeMenu(); vm.launch(app) }
                        MenuButton("Fermer") { vm.closeMenu() }
                    }
                    else -> {
                        if (vm.isFavorite(app)) {
                            MenuButton("Retirer de l'accueil", Modifier.focusRequester(first), danger = true) { vm.removeFromHome(app) }
                        } else {
                            MenuButton("Mettre sur l'accueil", Modifier.focusRequester(first)) { vm.addToHome(app) }
                        }
                        MenuButton("Ouvrir") { vm.closeMenu(); vm.launch(app) }
                        MenuButton("Fermer") { vm.closeMenu() }
                    }
                }
            }
            LaunchedEffect(menu) { kotlinx.coroutines.delay(60); runCatching { first.requestFocus() } }
        }
    }
}

/** Premier lancement : rendre la touche Home fidèle à Samoth Home. */
@Composable
fun SetupDialog(vm: HomeViewModel) {
    if (!vm.setupNeeded) return
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = { vm.dismissSetup() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(width = 460) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Faire de Samoth Home votre accueil", color = Samoth.Text, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text(
                    "Votre TV ouvre son accueil d'origine quand vous appuyez sur Home. " +
                        "Pour revenir toujours ici (touche Home, rallumage, retour d'une appli), activez une seule fois le service Samoth Home :",
                    color = Samoth.Muted, fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp,
                )
                Text(
                    "Accessibilité  →  Samoth Home – touche Home  →  Activer",
                    color = Samoth.Text, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                )
                Spacer(Modifier.height(4.dp))
                MenuButton("Ouvrir l'accessibilité", Modifier.focusRequester(first)) {
                    vm.dismissSetup()
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.onFailure {
                        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
                MenuButton("Plus tard") { vm.dismissSetup() }
            }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(60); runCatching { first.requestFocus() } }
        }
    }
}

/** Petit message en bas de l'écran (favori ajouté, retiré…). */
@Composable
fun Toast(text: String?, modifier: Modifier = Modifier) {
    if (text == null) return
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xF2141418))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(text, color = Samoth.Text, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}
