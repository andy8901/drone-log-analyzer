package com.neosky.servicesupport.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "neosky_app_prefs")

/**
 * Non-sensitive app preferences (never tokens — those live in [TokenManager]'s
 * EncryptedSharedPreferences). Backs things like the last-selected theme override and whether
 * the offline banner's dismiss-for-session state should persist.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_OVERRIDE = stringPreferencesKey("theme_override") // "system" | "light" | "dark"
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    }

    val themeOverride: Flow<String> = context.dataStore.data.map { it[Keys.THEME_OVERRIDE] ?: "system" }

    suspend fun setThemeOverride(value: String) {
        context.dataStore.edit { it[Keys.THEME_OVERRIDE] = value }
    }

    val onboardingSeen: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_SEEN] ?: false }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_SEEN] = seen }
    }
}
