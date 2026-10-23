package com.example.util

import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object LauncherIconManager {

    private const val LIGHT_ALIAS_CLASS = "com.example.LauncherLight"
    private const val DARK_ALIAS_CLASS = "com.example.LauncherDark"

    @Volatile
    private var lastKnownSystemDark: Boolean? = null

    fun onMainActivityResumed(context: Context, newConfig: Configuration? = null) {
        syncLauncherIcon(context, newConfig = newConfig ?: context.resources.configuration)
    }

    fun onMainActivityPausedOrStopped(context: Context, newConfig: Configuration? = null) {
        syncLauncherIcon(context, newConfig = newConfig ?: context.resources.configuration)
    }

    fun isDeviceInDarkMode(context: Context, newConfig: Configuration? = null): Boolean {
        if (newConfig != null) {
            val nightMask = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightMask == Configuration.UI_MODE_NIGHT_YES) {
                lastKnownSystemDark = true
                return true
            }
            if (nightMask == Configuration.UI_MODE_NIGHT_NO) {
                lastKnownSystemDark = false
                return false
            }
        }

        val ctxMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (ctxMask == Configuration.UI_MODE_NIGHT_YES) {
            lastKnownSystemDark = true
            return true
        }
        if (ctxMask == Configuration.UI_MODE_NIGHT_NO) {
            lastKnownSystemDark = false
            return false
        }

        val appMask = context.applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (appMask == Configuration.UI_MODE_NIGHT_YES) {
            lastKnownSystemDark = true
            return true
        }
        if (appMask == Configuration.UI_MODE_NIGHT_NO) {
            lastKnownSystemDark = false
            return false
        }

        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        when (uiModeManager?.nightMode) {
            UiModeManager.MODE_NIGHT_YES -> {
                lastKnownSystemDark = true
                return true
            }
            UiModeManager.MODE_NIGHT_NO -> {
                lastKnownSystemDark = false
                return false
            }
        }

        return lastKnownSystemDark ?: false
    }

    @Synchronized
    fun syncLauncherIcon(
        context: Context,
        newConfig: Configuration? = null,
        isDarkOverride: Boolean? = null
    ) {
        try {
            val appContext = context.applicationContext
            val pm = appContext.packageManager
            val pkg = appContext.packageName

            val lightAlias = ComponentName(pkg, LIGHT_ALIAS_CLASS)
            val darkAlias = ComponentName(pkg, DARK_ALIAS_CLASS)

            val isDark = if (isDarkOverride != null) {
                lastKnownSystemDark = isDarkOverride
                isDarkOverride
            } else {
                isDeviceInDarkMode(context, newConfig)
            }

            val lightState = pm.getComponentEnabledSetting(lightAlias)
            val darkState = pm.getComponentEnabledSetting(darkAlias)

            val needsUpdate = if (isDark) {
                darkState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    lightState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                lightState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    darkState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            if (!needsUpdate) {
                return
            }

            val aliasToEnable = if (isDark) darkAlias else lightAlias
            val aliasToDisable = if (isDark) lightAlias else darkAlias

            // Enable the target alias first so the package never has zero enabled LAUNCHER activities
            pm.setComponentEnabledSetting(
                aliasToEnable,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                aliasToDisable,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Throwable) {
            // Safeguard against OEM PackageManager quirks
        }
    }
}
