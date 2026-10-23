package com.samoth.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.samoth.home.ui.HomeRoot
import com.samoth.home.ui.SamothTheme

class MainActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    // Rallumage de la TV (sortie de veille) : retour à l'écran d'allumage « SAMOTH HOME »
    private val screenOn = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            vm.resetToStart()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = vm.back()
        })
        ContextCompat.registerReceiver(
            this, screenOn, IntentFilter(Intent.ACTION_SCREEN_ON), ContextCompat.RECEIVER_EXPORTED
        )
        vm.refresh()
        setContent { SamothTheme { HomeRoot(vm) } }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
        vm.checkUpdate()
        vm.refreshWeather()
        vm.updateSetup(HomeGuardService.isDefaultHome(this), HomeGuardService.isEnabled(this))
    }

    // Appui sur la touche Home alors que le lanceur est déjà ouvert
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.resetToStart()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOn) }
        super.onDestroy()
    }
}
