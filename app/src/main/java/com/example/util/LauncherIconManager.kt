package com.example.util

import android.content.Context
import android.content.res.Configuration

object LauncherIconManager {

    fun onMainActivityResumed(context: Context, newConfig: Configuration? = null) {
        // No-op to prevent process restarts
    }

    fun onMainActivityPausedOrStopped(context: Context, newConfig: Configuration? = null) {
        // No-op to prevent process restarts
    }

    fun isDeviceInDarkMode(context: Context, newConfig: Configuration? = null): Boolean {
        val cfg = newConfig ?: context.resources.configuration
        val mask = cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mask == Configuration.UI_MODE_NIGHT_YES
    }

    fun syncLauncherIcon(
        context: Context,
        newConfig: Configuration? = null,
        isDarkOverride: Boolean? = null
    ) {
        // Safe no-op to ensure 100% startup reliability and prevent process restarts
    }
}
