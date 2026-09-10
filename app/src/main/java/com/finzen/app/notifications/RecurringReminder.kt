package com.finzen.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.finzen.app.R
import com.finzen.app.data.repository.RecurringTransactionRepository
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import java.time.LocalDate

class RecurringReminder(
    private val context: Context,
    private val repository: RecurringTransactionRepository,
) {

    suspend fun notifyDue() {
        val manager = NotificationManagerCompat.from(context)
        ensureChannel()
        if (!manager.areNotificationsEnabled()) return

        repository.getDue().forEach { item ->
            val prefill = TransactionPrefill(
                type = item.type,
                amountCents = Math.round(item.amount * 100.0),
                description = item.description,
                categoryId = item.categoryId,
                accountId = item.accountId,
                creditCardId = item.creditCardId,
                recurringId = item.id,
            )
            val pendingIntent = PendingIntent.getActivity(
                context,
                item.id.toInt(),
                PrefillIntent.build(context, prefill),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Lançamento recorrente")
                .setContentText("${item.description} • ${Money.format(item.amount)} • ${dueLabel(item.nextDueDate)}")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            manager.notify(BASE_NOTIFICATION_ID + item.id.toInt(), notification)
        }
    }

    private fun dueLabel(dueMillis: Long): String {
        val date = DateUtils.toLocalDate(dueMillis)
        return if (date.isBefore(LocalDate.now())) {
            "venceu em ${DateUtils.dayMonthLabel(dueMillis)}"
        } else {
            "vence hoje"
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lançamentos recorrentes",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Lembra de confirmar contas e receitas que se repetem."
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "recurring_reminders"
        const val BASE_NOTIFICATION_ID = 5000
    }
}
