package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.viewmodel.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val title: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro")
}

enum class PaperThemeMode(val title: String) {
    LIGHT("Página blanca"),
    DARK("Página oscura"),
    AUTO("Automático")
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aura_notes_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _paperThemeMode = MutableStateFlow(getPaperThemeMode())
    val paperThemeMode: StateFlow<PaperThemeMode> = _paperThemeMode.asStateFlow()

    private val _isGridView = MutableStateFlow(isGridViewDefault())
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _defaultPaperStyle = MutableStateFlow(getDefaultPaperStyle())
    val defaultPaperStyle: StateFlow<String> = _defaultPaperStyle.asStateFlow()

    private val _defaultSortOption = MutableStateFlow(getDefaultSortOption())
    val defaultSortOption: StateFlow<SortOption> = _defaultSortOption.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(getGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _defaultLockPin = MutableStateFlow(getDefaultLockPin())
    val defaultLockPin: StateFlow<String> = _defaultLockPin.asStateFlow()

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun getPaperThemeMode(): PaperThemeMode {
        val name = prefs.getString(KEY_PAPER_THEME_MODE, PaperThemeMode.AUTO.name) ?: PaperThemeMode.AUTO.name
        return try {
            PaperThemeMode.valueOf(name)
        } catch (_: Exception) {
            PaperThemeMode.AUTO
        }
    }

    fun setPaperThemeMode(mode: PaperThemeMode) {
        prefs.edit().putString(KEY_PAPER_THEME_MODE, mode.name).apply()
        _paperThemeMode.value = mode
    }

    fun isGridViewDefault(): Boolean {
        return prefs.getBoolean(KEY_DEFAULT_VIEW_GRID, true)
    }

    fun setGridViewDefault(isGrid: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_VIEW_GRID, isGrid).apply()
        _isGridView.value = isGrid
    }

    fun getDefaultPaperStyle(): String {
        return prefs.getString(KEY_DEFAULT_PAPER_STYLE, "BLANK") ?: "BLANK"
    }

    fun setDefaultPaperStyle(style: String) {
        prefs.edit().putString(KEY_DEFAULT_PAPER_STYLE, style).apply()
        _defaultPaperStyle.value = style
    }

    fun getDefaultSortOption(): SortOption {
        val name = prefs.getString(KEY_DEFAULT_SORT, SortOption.MODIFIED_DESC.name) ?: SortOption.MODIFIED_DESC.name
        return try {
            SortOption.valueOf(name)
        } catch (_: Exception) {
            SortOption.MODIFIED_DESC
        }
    }

    fun setDefaultSortOption(option: SortOption) {
        prefs.edit().putString(KEY_DEFAULT_SORT, option.name).apply()
        _defaultSortOption.value = option
    }

    fun getGeminiApiKey(): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
        _geminiApiKey.value = key.trim()
    }

    fun getDefaultLockPin(): String {
        return prefs.getString(KEY_DEFAULT_LOCK_PIN, "1234") ?: "1234"
    }

    fun setDefaultLockPin(pin: String) {
        prefs.edit().putString(KEY_DEFAULT_LOCK_PIN, pin.trim()).apply()
        _defaultLockPin.value = pin.trim()
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_PAPER_THEME_MODE = "key_paper_theme_mode"
        private const val KEY_DEFAULT_VIEW_GRID = "key_default_view_grid"
        private const val KEY_DEFAULT_PAPER_STYLE = "key_default_paper_style"
        private const val KEY_DEFAULT_SORT = "key_default_sort"
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
        private const val KEY_DEFAULT_LOCK_PIN = "key_default_lock_pin"
    }
}
