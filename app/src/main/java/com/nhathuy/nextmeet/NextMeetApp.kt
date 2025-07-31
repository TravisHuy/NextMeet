package com.nhathuy.nextmeet

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate

@HiltAndroidApp
class NextMeetApp : Application() {
    companion object {
        private const val PREF_THEME = "pref_theme"
        private const val THEME_LIGHT = "light"
        private const val THEME_DARK = "dark"
        private const val THEME_SYSTEM = "system"
    }

    override fun onCreate() {
        super.onCreate()

        // Khôi phục theme đã lưu
        restoreThemeFromPreferences()
    }

    private fun restoreThemeFromPreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val savedTheme = sharedPreferences.getString(PREF_THEME, THEME_SYSTEM)

        val mode = when (savedTheme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }
}