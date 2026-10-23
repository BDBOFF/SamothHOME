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

/** Menu ouvert par un appui long sur OK. */
data class TileMenu(val app: AppEntry, val fromHome: Boolean, val choosingSlot: Boolean = false)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppsRepository(app)

    var apps by mutableStateOf<List<AppEntry>>(emptyList()); private set
    var loaded by mutableStateOf(false); private set
    var screen by mutableStateOf(Screen.Home); private set
    private var pinned by mutableStateOf(repo.pinned())

    /** false à l'allumage (grand « SAMOTH HOME »), true dès qu'on navigue. */
    var navigating by mutableStateOf(false); private set
    /** Appli dont le nom est affiché en fond (mise à jour avec un léger délai). */
    var markApp by mutableStateOf<AppEntry?>(null); private set
    /** Demande de remettre le focus sur la première tuile (allumage, touche Home). */
    var focusReset by mutableStateOf(0); private set
    /** Demande de remettre le focus sur une tuile précise de l'accueil après une modification. */
    var focusPkg by mutableStateOf<String?>(null)

    var update by mutableStateOf<UpdateState>(UpdateState.None); private set
    var menu by mutableStateOf<TileMenu?>(null); private set
    var toast by mutableStateOf<String?>(null); private set

    /** Configuration de la touche Home à proposer (service d'accessibilité pas encore activé). */
    var setupNeeded by mutableStateOf(false); private set
    private var setupDismissed = false

    private var focused: AppEntry? = null
    private var markJob: Job? = null
    private var toastJob: Job? = null
    private var lastCheck = 0L

    /** Les applis de l'accueil, exactement dans l'ordre choisi (5 places max, jamais réordonnées). */
    val favorites: List<AppEntry>
        get() {
            val byPkg = apps.associateBy { it.pkg }
            return pinned.mapNotNull { byPkg[it] }.take(5)
        }

    /** Places vides de l'accueil (tuile « + »). */
    val freeSlots: Int get() = (5 - favorites.size).coerceAtLeast(0)

    fun isFavorite(app: AppEntry) = favorites.any { it.pkg == app.pkg }

    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.Default) { repo.load() }
            apps = list
            loaded = true
            // Premier lancement : on remplit l'accueil une fois, ensuite il ne bouge plus
            if (!repo.isSeeded()) {
                if (pinned.isEmpty()) save(list.take(5).map { it.pkg })
                repo.markSeeded()
            }
        }
    }

    fun updateSetup(defaultHome: Boolean, guardOn: Boolean) {
        setupNeeded = !defaultHome && !guardOn && !setupDismissed
    }

    fun dismissSetup() {
        setupDismissed = true
        setupNeeded = false
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
    }

    fun openAllApps() { screen = Screen.AllApps }

    /** Bouton Retour : ferme le menu, ou revient à l'accueil ; sur l'accueil, on reste. */
    fun back() {
        when {
            menu != null -> endMenu()
            screen != Screen.Home -> screen = Screen.Home
        }
    }

    /** Touche Home ou rallumage de la TV : retour à l'écran d'allumage. */
    fun resetToStart() {
        menu = null
        screen = Screen.Home
        navigating = false
        markApp = null
        focusReset++
    }

    // ---- Favoris (appui long) ----

    fun openMenu(app: AppEntry, fromHome: Boolean) { menu = TileMenu(app, fromHome) }
    fun closeMenu() = endMenu()

    /** Ferme le menu et redonne le focus à la tuile concernée. */
    private fun endMenu() {
        focusPkg = menu?.app?.pkg
        menu = null
    }

    /** Ordre actuel de l'accueil (applis encore installées). */
    private fun current(): MutableList<String> = favorites.map { it.pkg }.toMutableList()

    private fun save(list: List<String>) {
        pinned = list.distinct().take(5)
        repo.savePinned(pinned)
    }

    fun addToHome(app: AppEntry) {
        val list = current()
        if (app.pkg in list) { endMenu(); return }
        if (list.size < 5) {
            list.add(app.pkg); save(list); endMenu()
            showToast("${app.label} ajoutée à l'accueil")
        } else {
            menu = TileMenu(app, fromHome = false, choosingSlot = true)
        }
    }

    fun replaceSlot(index: Int, app: AppEntry) {
        val list = current()
        if (index !in list.indices) return
        val old = apps.firstOrNull { it.pkg == list[index] }?.label
        list[index] = app.pkg
        save(list)
        endMenu()
        showToast(if (old != null) "${app.label} remplace $old" else "${app.label} ajoutée à l'accueil")
    }

    fun removeFromHome(app: AppEntry) {
        val list = current()
        list.remove(app.pkg)
        save(list)
        endMenu()
        showToast("${app.label} retirée de l'accueil")
    }

    fun move(app: AppEntry, delta: Int) {
        val list = current()
        val i = list.indexOf(app.pkg)
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        list.removeAt(i); list.add(j, app.pkg)
        save(list)
        focusPkg = app.pkg
        // Le menu reste ouvert pour enchaîner les déplacements
        menu = TileMenu(app, fromHome = true)
    }

    // ---- Gestion des applis ----

    fun uninstall(app: AppEntry) {
        endMenu()
        if (!repo.uninstall(app)) showToast("Impossible de désinstaller ${app.label}")
        // La liste se met à jour au retour (onResume)
    }

    fun openDetails(app: AppEntry) {
        endMenu()
        if (!repo.openDetails(app)) showToast("Réglages de l'appli indisponibles")
    }

    fun forceStop(app: AppEntry) {
        endMenu()
        viewModelScope.launch(Dispatchers.Default) { repo.kill(app.pkg) }
        showToast("${app.label} fermée : elle repartira à neuf")
    }

    fun closeAll() {
        val pkgs = apps.map { it.pkg }
        viewModelScope.launch(Dispatchers.Default) { pkgs.forEach { repo.kill(it) } }
        showToast("Toutes les applis ont été fermées")
    }

    fun positionOf(app: AppEntry) = favorites.indexOfFirst { it.pkg == app.pkg }

    private fun showToast(text: String) {
        toast = text
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(2500); toast = null }
    }

    // ---- Mise à jour ----

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
