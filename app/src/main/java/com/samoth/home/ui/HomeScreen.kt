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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    }
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

    LaunchedEffect(vm.focusReset, favorites.isNotEmpty()) {
        if (favorites.isNotEmpty()) runCatching { first.requestFocus() }
    }

    val mark = vm.markApp
    val markText = if (vm.navigating && mark != null) mark.label.uppercase(Locale.FRANCE) else "SAMOTH HOME"
    val markColor = if (vm.navigating && mark != null) mark.color else Samoth.Red

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(start = 80.dp, end = 80.dp, top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopActions(vm, Modifier.align(Alignment.End))

            Text(
                formatTime(now),
                color = androidx.compose.ui.graphics.Color.White,
                fontFamily = Inter, fontWeight = FontWeight.Light,
                fontSize = 80.sp, lineHeight = 80.sp, letterSpacing = (-0.04).em,
            )
            Text(
                formatDate(now),
                color = Samoth.Muted, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp,
            )

            Watermark(markText, markColor, height = 132.dp, modifier = Modifier.padding(top = 4.dp))

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                favorites.forEachIndexed { i, app ->
                    AppTile(
                        app,
                        modifier = Modifier.weight(1f),
                        focusModifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                        onFocused = { vm.onFocus(app) },
                        onClick = { vm.launch(app) },
                    )
                }
                // Garde des tuiles de même taille s'il y a moins de 5 applis
                repeat(5 - favorites.size) { Spacer(Modifier.weight(1f)) }
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
