package com.finzen.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.finzen.app.R
import com.finzen.app.data.repository.BudgetRepository
import com.finzen.app.data.repository.SettingsRepository
import com.finzen.app.ui.navigation.Routes
import com.finzen.app.util.MonthRef
import com.finzen.app.util.Money
import kotlinx.coroutines.flow.first

class BudgetReminder(
    private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun notifyAlerts() {
        val manager = NotificationManagerCompat.from(context)
        ensureChannel()
        if (!manager.areNotificationsEnabled()) return

        val month = MonthRef.now()
        val progress = budgetRepository.observeProgress(month.year, month.month).first()
        val fired = settingsRepository.firedBudgetAlerts()

        progress.forEach { p ->
            if (p.budget <= 0.0) return@forEach
            val ratio = p.spent / p.budget
            val threshold = when {
                ratio >= 1.0 -> 100
                ratio >= 0.8 -> 80
                else -> return@forEach
            }
            val key = "budget:${p.category.id}:${month.year}:${month.month}:$threshold"
            if (key in fired) return@forEach

            val title: String
            val text: String
            if (threshold == 100) {
                title = "Orçamento estourado"
                text = "${p.category.name}: gastou ${Money.format(p.spent)} de ${Money.format(p.budget)}."
            } else {
                title = "Orçamento quase no limite"
                text = "${p.category.name}: ${(ratio * 100).toInt()}% de ${Money.format(p.budget)} já usados."
            }

            val notifId = BASE_NOTIFICATION_ID + p.category.id.toInt() * 2 + (if (threshold == 100) 1 else 0)
            val pendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                NavIntent.build(context, Routes.BUDGETS),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            manager.notify(notifId, notification)
            settingsRepository.addFiredBudgetAlert(key)
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de orçamento",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisa quando um orçamento de categoria atinge 80% ou 100%."
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val BASE_NOTIFICATION_ID = 7000
    }
}
