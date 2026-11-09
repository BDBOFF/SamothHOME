package com.samoth.home

import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Écran de veille (fond d'écran Google) : quand il se ferme, certaines TV repassent par l'accueil
 * au lieu de revenir à l'appli en cours. On retient l'appli qui était à l'écran quand l'écran
 * de veille a démarré, et on la rouvre (elle reprend là où elle était, en pause).
 */
object ResumeAfterDream {
    @Volatile var dreaming = false
    @Volatile private var savedPkg: String? = null
    @Volatile private var dreamEndedAt = 0L

    private const val WINDOW_MS = 30_000L

    fun onDreamStarted(foregroundApp: String?) {
        dreaming = true
        if (foregroundApp != null) savedPkg = foregroundApp
    }

    fun onDreamStopped() {
        dreaming = false
        dreamEndedAt = SystemClock.elapsedRealtime()
    }

    /** Vraie mise en veille de la TV, touche Home, ou l'appli est revenue toute seule : rien à rouvrir. */
    fun clear() {
        savedPkg = null
    }

    private fun pending(): String? = savedPkg?.takeIf {
        !dreaming && SystemClock.elapsedRealtime() - dreamEndedAt < WINDOW_MS
    }

    /** Rouvre l'appli interrompue par l'écran de veille, si c'est le cas. Renvoie true si fait. */
    fun restoreIfNeeded(context: Context): Boolean {
        val pkg = pending() ?: return false
        savedPkg = null
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return false
        return runCatching {
            // Comme un lanceur : ramène la tâche existante au premier plan, sans la recréer
            context.startActivity(
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            )
            true
        }.getOrDefault(false)
    }
}
