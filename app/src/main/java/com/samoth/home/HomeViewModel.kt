package com.samoth.home

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Home, AllApps }

sealed interface UpdateState {
    data object None : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class Message(val text: String, val info: UpdateInfo?) : UpdateState
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppsRepository(app)

    var apps by mutableStateOf<List<AppEntry>>(emptyList()); private set
    var loaded by mutableStateOf(false); private set
    var screen by mutableStateOf(Screen.Home); private set

    /** false à l'allumage (grand « SAMOTH HOME »), true dès qu'on navigue. */
    var navigating by mutableStateOf(false); private set
    /** Appli dont le nom est affiché en fond (mise à jour avec un léger délai). */
    var markApp by mutableStateOf<AppEntry?>(null); private set
    /** Demande de remettre le focus sur la première tuile (allumage, touche Home). */
    var focusReset by mutableStateOf(0); private set

    var update by mutableStateOf<UpdateState>(UpdateState.None); private set

    private var focused: AppEntry? = null
    private var markJob: Job? = null
    private var lastCheck = 0L

    val favorites: List<AppEntry> get() = apps.take(5)

    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.Default) { repo.load() }
            apps = list
            loaded = true
        }
    }

    fun onFocus(app: AppEntry) {
        focused = app
        if (!navigating) return
        markJob?.cancel()
        markJob = viewModelScope.launch {
            delay(220) // évite que le fond clignote quand on défile vite
            markApp = app
        }
    }

    /** Appelé à chaque appui sur une flèche de la télécommande. */
    fun onNavigate() {
        if (navigating) return
        navigating = true
        focused?.let { markApp = it }
    }

    fun launch(app: AppEntry) {
        repo.launch(app)
        // Le tri par utilisation est appliqué au retour sur l'accueil (onResume → refresh)
    }

    fun openAllApps() { screen = Screen.AllApps }

    /** Bouton Retour : revient à l'accueil ; sur l'accueil, ne fait rien (on reste sur le lanceur). */
    fun back() {
        if (screen != Screen.Home) screen = Screen.Home
    }

    /** Touche Home ou rallumage de la TV : retour à l'écran d'allumage. */
    fun resetToStart() {
        screen = Screen.Home
        navigating = false
        markApp = null
        focusReset++
    }

    fun checkUpdate(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastCheck < 30 * 60_000L && lastCheck != 0L) return
        if (update is UpdateState.Downloading) return
        lastCheck = now
        viewModelScope.launch {
            val info = Updater.check()
            update = if (info != null) UpdateState.Available(info) else UpdateState.None
        }
    }

    fun runUpdate() {
        val ctx = getApplication<Application>()
        val info = when (val u = update) {
            is UpdateState.Available -> u.info
            is UpdateState.Message -> u.info
            else -> null
        } ?: return
        viewModelScope.launch {
            try {
                update = UpdateState.Downloading(0f)
                val file = Updater.download(ctx, info) { p -> update = UpdateState.Downloading(p) }
                update = if (Updater.install(ctx, file)) UpdateState.Available(info)
                else UpdateState.Message("Autorisez Samoth Home à installer, puis réessayez", info)
            } catch (e: Exception) {
                update = UpdateState.Message("Échec du téléchargement, réessayez", info)
            }
        }
    }
}
