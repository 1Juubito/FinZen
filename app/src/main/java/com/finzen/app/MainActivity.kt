package com.finzen.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.finzen.app.data.repository.ThemeMode
import com.finzen.app.notifications.AppNavBus
import com.finzen.app.notifications.NavIntent
import com.finzen.app.notifications.PrefillIntent
import com.finzen.app.notifications.TransactionPrefillBus
import com.finzen.app.shortcuts.QuickActionIntent
import com.finzen.app.ui.navigation.MainScaffold
import com.finzen.app.ui.security.BiometricGate
import com.finzen.app.ui.theme.FinZenTheme
import com.finzen.app.widget.BalanceWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleNotificationIntent(intent)

        val container = (application as FinanceApp).container
        val settingsRepository = container.settingsRepository

        val initialBiometric = runBlocking { settingsRepository.biometricLockEnabled.first() }

        lifecycleScope.launch {
            runCatching { container.backupManager.runAutoBackupIfDue() }
        }

        lifecycleScope.launch {
            runCatching { container.recurringReminder.notifyDue() }
        }
        lifecycleScope.launch {
            runCatching { container.invoiceReminder.notifyDue() }
        }
        lifecycleScope.launch {
            runCatching { container.budgetReminder.notifyAlerts() }
        }

        setContent {
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val biometricEnabled by settingsRepository.biometricLockEnabled
                .collectAsStateWithLifecycle(initialValue = initialBiometric)

            FinZenTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    BiometricGate(enabled = biometricEnabled) {
                        MainScaffold()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        PrefillIntent.parse(intent)?.let { TransactionPrefillBus.submit(it) }
        NavIntent.route(intent)?.let { AppNavBus.submit(it) }
        QuickActionIntent.routeFor(intent)?.let { AppNavBus.submit(it) }
    }

    override fun onStop() {
        super.onStop()

        BalanceWidgetProvider.refresh(this)
    }
}
