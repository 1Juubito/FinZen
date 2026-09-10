package com.finzen.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finzen_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val BALANCE_VISIBLE = booleanPreferencesKey("balance_visible")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FOLDER = stringPreferencesKey("auto_backup_folder")
        val AUTO_BACKUP_AT = longPreferencesKey("auto_backup_at")
        val CAPTURE_PURCHASES = booleanPreferencesKey("capture_purchases_enabled")
        val USER_NAME = stringPreferencesKey("user_name")
        val BUDGET_ALERTS = stringSetPreferencesKey("budget_alerts_fired")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val balanceVisible: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BALANCE_VISIBLE] ?: true
    }

    val biometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_LOCK] ?: false
    }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_BACKUP] ?: false
    }

    val autoBackupFolder: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_BACKUP_FOLDER]
    }

    val lastAutoBackupAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_BACKUP_AT] ?: 0L
    }

    val capturePurchasesEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CAPTURE_PURCHASES] ?: false
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_NAME].orEmpty()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setBalanceVisible(visible: Boolean) {
        context.dataStore.edit { it[Keys.BALANCE_VISIBLE] = visible }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP] = enabled }
    }

    suspend fun setAutoBackupFolder(uri: String) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_FOLDER] = uri }
    }

    suspend fun setLastAutoBackupAt(at: Long) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_AT] = at }
    }

    suspend fun setCapturePurchasesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CAPTURE_PURCHASES] = enabled }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim() }
    }

    suspend fun firedBudgetAlerts(): Set<String> =
        context.dataStore.data.first()[Keys.BUDGET_ALERTS] ?: emptySet()

    suspend fun addFiredBudgetAlert(key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUDGET_ALERTS] = (prefs[Keys.BUDGET_ALERTS] ?: emptySet()) + key
        }
    }
}
