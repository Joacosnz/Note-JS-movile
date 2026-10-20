package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.example.MainActivity

abstract class BaseQuickActionTileService : TileService() {

    abstract val actionString: String

    override fun onClick() {
        super.onClick()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            action = actionString
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

class NewNoteTileService : BaseQuickActionTileService() {
    override val actionString: String = MainActivity.ACTION_QUICK_NOTE
}

class VoiceNoteTileService : BaseQuickActionTileService() {
    override val actionString: String = MainActivity.ACTION_QUICK_VOICE
}

class ScanDocTileService : BaseQuickActionTileService() {
    override val actionString: String = MainActivity.ACTION_QUICK_SCAN
}