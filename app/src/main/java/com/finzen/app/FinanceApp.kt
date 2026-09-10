package com.finzen.app

import android.app.Application
import com.finzen.app.data.seed.DatabaseSeeder
import com.finzen.app.di.AppContainer
import com.finzen.app.di.AppDataContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinanceApp : Application() {

    lateinit var container: AppContainer
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        container = AppDataContainer(this)
        applicationScope.launch {
            DatabaseSeeder.seedDefaultsIfEmpty(container.database)
        }

        applicationScope.launch {
            runCatching {
                val cards = container.creditCardRepository.getAll()
                container.transactionRepository.recomputeCardInvoiceDates(cards)
            }
        }
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                getSharedPreferences("finzen_diag", MODE_PRIVATE).edit()
                    .putString("last_crash", throwable.stackTraceToString())
                    .putLong("last_crash_at", System.currentTimeMillis())
                    .commit()
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
