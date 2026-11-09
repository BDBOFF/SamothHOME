package com.samoth.home

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

/**
 * Sur les TV Android, la touche Home ouvre l'accueil du constructeur et ne propose pas de choix.
 * Ce service (à activer une fois dans Accessibilité) détecte l'ouverture de cet accueil
 * et ramène aussitôt sur Samoth Home : touche Home, sortie de veille, retour d'une appli.
 */
class HomeGuardService : AccessibilityService() {

    private var lastJump = 0L

    /** Dernière appli « normale » vue à l'écran, et si c'est encore elle qui est devant. */
    private var lastApp: String? = null
    private var appInFront = false
    private val launchable = HashMap<String, Boolean>()

    private val screenEvents = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_DREAMING_STARTED ->
                    ResumeAfterDream.onDreamStarted(if (appInFront) lastApp else null)
                Intent.ACTION_DREAMING_STOPPED -> ResumeAfterDream.onDreamStopped()
                // Vraie mise en veille : au rallumage, on veut l'accueil
                Intent.ACTION_SCREEN_OFF -> {
                    ResumeAfterDream.clear()
                    ResumeAfterDream.dreaming = false
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ContextCompat.registerReceiver(
            this, screenEvents,
            IntentFilter().apply {
                addAction(Intent.ACTION_DREAMING_STARTED)
                addAction(Intent.ACTION_DREAMING_STOPPED)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenEvents) }
        super.onDestroy()
    }

    private fun isLaunchable(pkg: String): Boolean = launchable.getOrPut(pkg) {
        val pm = packageManager
        pm.getLeanbackLaunchIntentForPackage(pkg) != null || pm.getLaunchIntentForPackage(pkg) != null
    }

    /**
     * Touche Home interceptée avant que la TV n'ouvre son propre accueil :
     * Samoth Home s'affiche immédiatement, sans passer par l'écran Sony.
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
            ResumeAfterDream.clear() // Home voulu : on ne rouvre rien
            goHome()
        }
        return true
    }

    private fun goHome() {
        lastJump = SystemClock.elapsedRealtime()
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .setAction(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Pendant l'écran de veille, on ne touche à rien
        if (ResumeAfterDream.dreaming) return
        if (pkg == packageName) { appInFront = false; return }
        if (pkg in stockLaunchers(this)) {
            appInFront = false
            // Fin d'écran de veille : on rouvre l'appli en cours plutôt que l'accueil
            if (ResumeAfterDream.restoreIfNeeded(this)) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastJump < 800) return
            goHome()
            return
        }
        if (isLaunchable(pkg)) {
            lastApp = pkg
            appInFront = true
            ResumeAfterDream.clear() // l'appli est revenue d'elle-même
        }
    }

    override fun onInterrupt() {}

    companion object {
        /** Accueils d'origine connus (Android TV, Google TV, Sony). */
        private val KNOWN = setOf(
            "com.google.android.tvlauncher",
            "com.google.android.leanbacklauncher",
            "com.google.android.apps.tv.launcherx",
            "com.sony.dtv.tvlauncher",
            "com.sony.dtv.home",
        )

        private var cached: Set<String>? = null

        fun stockLaunchers(context: Context): Set<String> {
            cached?.let { return it }
            val pm = context.packageManager
            val homes = runCatching {
                pm.queryIntentActivities(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                    PackageManager.MATCH_ALL,
                ).mapNotNull { it.activityInfo?.packageName }
            }.getOrDefault(emptyList())
            // Les « accueils » installés, sauf nous et le petit écran de secours des réglages
            val set = (KNOWN + homes)
                .filter { it != context.packageName && it != "com.android.tv.settings" && it != "com.android.settings" }
                .toSet()
            cached = set
            return set
        }

        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, HomeGuardService::class.java).flattenToString()
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return list.split(':').any { it.equals(me, ignoreCase = true) }
        }

        fun isDefaultHome(context: Context): Boolean {
            val ri = context.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY,
            )
            return ri?.activityInfo?.packageName == context.packageName
        }
    }
}
