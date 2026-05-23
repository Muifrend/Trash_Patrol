package com.example

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("trash_patrol_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_COORDINATOR_URL = "coordinator_url"
        private const val DEFAULT_COORDINATOR_URL = "https://pachydermatous-alena-hyperbarbarous.ngrok-free.dev"
    }

    var coordinatorUrl: String
        get() = prefs.getString(KEY_COORDINATOR_URL, DEFAULT_COORDINATOR_URL) ?: DEFAULT_COORDINATOR_URL
        set(value) {
            prefs.edit().putString(KEY_COORDINATOR_URL, value.trim()).apply()
        }
}
