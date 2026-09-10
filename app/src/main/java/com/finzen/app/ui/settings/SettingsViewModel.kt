package com.finzen.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.backup.BackupManager
import com.finzen.app.data.local.FinanceDatabase
import com.finzen.app.data.repository.SettingsRepository
import com.finzen.app.data.repository.ThemeMode
import com.finzen.app.data.seed.DatabaseSeeder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val database: FinanceDatabase,
    private val backupManager: BackupManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val biometricLockEnabled: StateFlow<Boolean> = settingsRepository.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoBackupEnabled: StateFlow<Boolean> = settingsRepository.autoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val capturePurchasesEnabled: StateFlow<Boolean> = settingsRepository.capturePurchasesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val userName: StateFlow<String> = settingsRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val lastAutoBackupAt: StateFlow<Long> = settingsRepository.lastAutoBackupAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricLockEnabled(enabled) }
    }

    fun setCapturePurchases(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCapturePurchasesEnabled(enabled) }
    }

    fun setUserName(name: String) {
        viewModelScope.launch { settingsRepository.setUserName(name) }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            _isBusy.value = true
            DatabaseSeeder.loadSampleData(database)
            _isBusy.value = false
        }
    }

    fun clearTransactions() {
        viewModelScope.launch {
            _isBusy.value = true
            DatabaseSeeder.clearTransactions(database)
            _isBusy.value = false
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = runCatching { backupManager.export(uri) }
            _isBusy.value = false
            _message.value = result.fold(
                onSuccess = { "Backup salvo com sucesso ($it transações)." },
                onFailure = { "Não foi possível exportar: ${it.message ?: "erro desconhecido"}" },
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = runCatching { backupManager.import(uri) }
            _isBusy.value = false
            _message.value = result.fold(
                onSuccess = { "Backup restaurado ($it transações)." },
                onFailure = { "Não foi possível importar. Verifique se o arquivo é um backup válido." },
            )
        }
    }

    fun enableAutoBackup(folderUri: String) {
        viewModelScope.launch {
            settingsRepository.setAutoBackupFolder(folderUri)
            settingsRepository.setAutoBackupEnabled(true)
            _isBusy.value = true
            val wrote = runCatching { backupManager.runAutoBackupIfDue(force = true) }.getOrDefault(false)
            _isBusy.value = false
            _message.value = if (wrote) {
                "Backup automático ativado. Primeira cópia salva."
            } else {
                "Backup automático ativado."
            }
        }
    }

    fun disableAutoBackup() {
        viewModelScope.launch {
            settingsRepository.setAutoBackupEnabled(false)
            _message.value = "Backup automático desativado."
        }
    }
}
