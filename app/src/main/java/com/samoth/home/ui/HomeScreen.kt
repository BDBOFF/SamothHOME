package com.samoth.home.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.samoth.home.HomeViewModel
import com.samoth.home.Screen
import com.samoth.home.UpdateState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeRoot(vm: HomeViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Samoth.Bg)
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key in NavKeys) vm.onNavigate()
                false
            }
    ) {
        when (vm.screen) {
            Screen.Home -> HomeScreen(vm)
            Screen.AllApps -> AllAppsScreen(vm)
        }
        Toast(vm.toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))
        TileMenuDialog(vm)
        SetupDialog(vm)
    }
}

/** Une FocusRequester par appli, pour redonner le focus à la bonne tuile. */
@Composable
fun rememberTileFocus(vm: HomeViewModel, visible: List<String>, fallback: FocusRequester): Map<String, FocusRequester> {
    val map = remember { mutableMapOf<String, FocusRequester>() }
    visible.forEach { pkg -> if (pkg !in map) map[pkg] = FocusRequester() }
    LaunchedEffect(vm.focusPkg, vm.menu) {
        val pkg = vm.focusPkg ?: return@LaunchedEffect
        if (vm.menu != null) return@LaunchedEffect
        delay(80)
        val target = if (pkg in visible) map[pkg] else fallback
        runCatching { target?.requestFocus() }
        vm.focusPkg = null
    }
    return map
}

private val NavKeys = setOf(Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown)

@Composable
fun rememberNow(): Date {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000L * (60 - (now.time / 1000 % 60)).coerceAtLeast(1))
        }
    }
    return now
}

private val TimeFmt = SimpleDateFormat("HH:mm", Locale.FRANCE)
private val DateFmt = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)

fun formatTime(d: Date): String = TimeFmt.format(d)
fun formatDate(d: Date): String = DateFmt.format(d).replaceFirstChar { it.titlecase(Locale.FRANCE) }

@Composable
fun TopActions(vm: HomeViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val u = vm.update) {
            is UpdateState.Available -> GlassPill("Mise à jour disponible", accent = Samoth.Red) { vm.runUpdate() }
            is UpdateState.Downloading -> GlassPill("Téléchargement ${(u.progress * 100).toInt()} %", accent = Samoth.Red) {}
            is UpdateState.Message -> GlassPill(u.text, accent = Samoth.Red) { vm.runUpdate() }
            UpdateState.None -> {}
        }
        SettingsButton {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val now = rememberNow()
    val first = remember { FocusRequester() }
    val favorites = vm.favorites
    val tileFocus = rememberTileFocus(vm, favorites.map { it.pkg }, first)

    LaunchedEffect(vm.focusReset, favorites.isNotEmpty()) {
        if (favorites.isNotEmpty()) runCatching { first.requestFocus() }
    }

    val mark = vm.markApp
    val markText = if (vm.navigating && mark != null) mark.label.uppercase(Locale.FRANCE) else "SAMOTH HOME"
    val markColor = if (vm.navigating && mark != null) mark.color else Samoth.Red

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(start = 80.dp, end = 80.dp, top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopActions(vm, Modifier.align(Alignment.End))

            Text(
                formatTime(now),
                color = Color.White,
                fontFamily = Inter, fontWeight = FontWeight.Light,
                fontSize = 108.sp, lineHeight = 108.sp, letterSpacing = (-0.04).em,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDate(now),
                    color = Color(0xFFC7C7D1), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 22.sp,
                )
                vm.weather?.let { w ->
                    Text(
                        "   ·   ",
                        color = Color(0xFF6B6B7A), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 22.sp,
                    )
                    WeatherIcon(w.sky, w.isDay, Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${w.temp}°",
                        color = Color.White, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${if (!w.isDay && w.sky == com.samoth.home.Sky.Clear) "Ciel dégagé" else w.label}  ·  ${w.min}° / ${w.max}°",
                        color = Color(0xFFA3A3B2), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 18.sp,
                    )
                }
            }

            Watermark(markText, markColor, height = 112.dp, modifier = Modifier.padding(top = 2.dp))

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                favorites.forEachIndexed { i, app ->
                    key(app.pkg) {
                        AppTile(
                            app,
                            modifier = Modifier.weight(1f),
                            focusModifier = Modifier
                                .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                                .focusRequester(tileFocus.getValue(app.pkg)),
                            onFocused = { vm.onFocus(app) },
                            onLongClick = { vm.openMenu(app, fromHome = true) },
                            onClick = { vm.launch(app) },
                        )
                    }
                }
                // Places libres : tuile « + » qui ouvre Toutes les applis
                if (vm.loaded) repeat(vm.freeSlots) {
                    AddTile(Modifier.weight(1f)) { vm.openAllApps() }
                }
            }

            Spacer(Modifier.height(18.dp))
            GlassPill("Toutes les applis", leading = { GridGlyph(it) }) { vm.openAllApps() }
        }

        AnimatedVisibility(
            visible = vm.navigating,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 32.dp, bottom = 18.dp),
        ) {
            Text(
                "SAMOTH HOME",
                color = Samoth.Red.copy(alpha = 0.6f),
                fontFamily = Unbounded, fontSize = 10.sp, letterSpacing = 0.08.em,
            )
        }
    }
}
