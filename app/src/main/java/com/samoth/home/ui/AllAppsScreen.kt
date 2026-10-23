package com.samoth.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.samoth.home.HomeViewModel

/** Toutes les applis installées, les plus utilisées d'abord, dans une grille qui défile. */
@Composable
fun AllAppsScreen(vm: HomeViewModel) {
    val now = rememberNow()
    val first = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    val apps = vm.apps
    val tileFocus = rememberTileFocus(vm, apps.map { it.pkg }, first)

    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) runCatching { first.requestFocus() }
    }

    Column(Modifier.fillMaxSize().padding(start = 70.dp, end = 70.dp, top = 28.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BackButton { vm.back() }
            Spacer(Modifier.width(14.dp))
            Text(
                "Toutes les applis",
                color = Samoth.Text, fontFamily = Inter, fontWeight = FontWeight.Bold,
                fontSize = 26.sp, letterSpacing = (-0.02).em,
            )
            Spacer(Modifier.weight(1f))
            GlassPill("Tout fermer") { vm.closeAll() }
            Spacer(Modifier.width(18.dp))
            Text(
                formatTime(now),
                color = Samoth.Text, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            )
            Spacer(Modifier.width(14.dp))
            TopActions(vm)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            state = grid,
            modifier = Modifier.fillMaxSize(),
            // Marges pour laisser respirer le halo ; la rangée suivante dépasse en bas de l'écran
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(apps, key = { _, a -> a.pkg }) { i, app ->
                AppTile(
                    app,
                    alwaysShowLabel = true,
                    focusModifier = Modifier
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                        .focusRequester(tileFocus.getValue(app.pkg)),
                    onFocused = { vm.onFocus(app) },
                    onLongClick = { vm.openMenu(app, fromHome = false) },
                    onClick = { vm.launch(app) },
                )
            }
        }
    }
}
