package com.finzen.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.finzen.app.R
import com.finzen.app.data.card.CardCycleCalculator
import com.finzen.app.data.repository.CreditCardRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.ui.navigation.Routes
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class InvoiceReminder(
    private val context: Context,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
) {

    suspend fun notifyDue() {
        val manager = NotificationManagerCompat.from(context)
        ensureChannel()
        if (!manager.areNotificationsEnabled()) return

        val today = LocalDate.now()
        creditCardRepository.getAll().forEach { card ->

            val cycle = CardCycleCalculator.cycleForOffset(today, card.closingDay, card.dueDay, -1)
            val daysUntilDue = ChronoUnit.DAYS.between(today, cycle.dueDate)
            if (daysUntilDue !in -OVERDUE_GRACE..REMIND_WINDOW) return@forEach

            val unpaid = transactionRepository.unpaidCardTotal(
                card.id,
                DateUtils.localDateToMillis(cycle.periodStart),
                DateUtils.localDateToMillis(cycle.periodEndExclusive),
            )
            if (unpaid <= 0.0) return@forEach

            val status = when {
                daysUntilDue > 1 -> "Vence em $daysUntilDue dias"
                daysUntilDue == 1L -> "Vence amanhã"
                daysUntilDue == 0L -> "Vence hoje"
                daysUntilDue == -1L -> "Venceu ontem"
                else -> "Venceu há ${-daysUntilDue} dias"
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                card.id.toInt(),
                NavIntent.build(context, Routes.cardDetail(card.id)),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Fatura do ${card.name}")
                .setContentText("$status • ${Money.format(unpaid)}")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            manager.notify(BASE_NOTIFICATION_ID + card.id.toInt(), notification)
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vencimento de faturas",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisa quando a fatura do cartão está perto de vencer."
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "invoice_reminders"
        const val BASE_NOTIFICATION_ID = 6000
        const val REMIND_WINDOW = 5L
        const val OVERDUE_GRACE = 3L
    }
}
