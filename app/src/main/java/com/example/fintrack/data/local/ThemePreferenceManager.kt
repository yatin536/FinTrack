package com.example.fintrack.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

enum class GlassThemePreset(val displayName: String, val subtitle: String) {
    MIDNIGHT_GLASS("Midnight Glass", "Deep obsidian with cyan & indigo neon glow"),
    AURO_GLASS("Auro Glass", "Dreamy pastel violet & magenta aurora"),
    SUNSET_GLOW("Sunset Glow", "Twilight plum with coral & warm amber embers"),
    EMERALD_CALM("Emerald Calm", "Deep forest teal with refreshing mint highlights")
}

class ThemePreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "fintrack_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_GLASS_PRESET = "glass_preset"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        const val DEFAULT_CURRENCY_SYMBOL = "₹"
    }

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _glassPreset = MutableStateFlow(getSavedGlassPreset())
    val glassPreset: StateFlow<GlassThemePreset> = _glassPreset.asStateFlow()

    private val _currencySymbol = MutableStateFlow(getSavedCurrencySymbol())
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private fun getSavedThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(saved)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

    private fun getSavedGlassPreset(): GlassThemePreset {
        val saved = prefs.getString(KEY_GLASS_PRESET, GlassThemePreset.MIDNIGHT_GLASS.name)
            ?: GlassThemePreset.MIDNIGHT_GLASS.name
        return try {
            GlassThemePreset.valueOf(saved)
        } catch (_: Exception) {
            GlassThemePreset.MIDNIGHT_GLASS
        }
    }

    private fun getSavedCurrencySymbol(): String {
        return prefs.getString(KEY_CURRENCY_SYMBOL, DEFAULT_CURRENCY_SYMBOL) ?: DEFAULT_CURRENCY_SYMBOL
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setGlassPreset(preset: GlassThemePreset) {
        prefs.edit().putString(KEY_GLASS_PRESET, preset.name).apply()
        _glassPreset.value = preset
    }

    fun setCurrencySymbol(symbol: String) {
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
        _currencySymbol.value = symbol
    }

    fun toggleDarkMode() {
        val newMode = when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(newMode)
    }
}
