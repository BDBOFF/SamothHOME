package com.samoth.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Filet de sécurité : au démarrage de la TV, ouvre Samoth Home. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            )
        }
    }
}
