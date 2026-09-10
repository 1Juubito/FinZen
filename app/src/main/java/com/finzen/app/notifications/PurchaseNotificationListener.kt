package com.finzen.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.finzen.app.FinanceApp
import com.finzen.app.R
import com.finzen.app.data.model.TransactionType
import com.finzen.app.util.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PurchaseNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var captureEnabled = false
    private var nextNotificationId = BASE_NOTIFICATION_ID

    override fun onListenerConnected() {
        super.onListenerConnected()
        ensureChannel()
        val settings = (application as FinanceApp).container.settingsRepository
        scope.launch {
            settings.capturePurchasesEnabled.collectLatest { captureEnabled = it }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!captureEnabled) return
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        val parsed = PurchaseParser.parse(title, bigText ?: text) ?: return
        notifyPurchase(parsed)
    }

    private fun notifyPurchase(parsed: PurchaseParser.Parsed) {
        val intent = PrefillIntent.build(
            this,
            TransactionPrefill(
                type = TransactionType.EXPENSE,
                amountCents = parsed.amountCents,
                description = parsed.description,
            ),
        )
        val pendingIntent = PendingIntent.getActivity(
            this,
            parsed.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val amount = Money.format(parsed.amountCents / 100.0)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Adicionar despesa?")
            .setContentText("${parsed.description} • $amount")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(nextNotificationId++, notification)
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Compras detectadas",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisa quando uma compra é detectada nas notificações do banco."
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "purchase_capture"
        private const val BASE_NOTIFICATION_ID = 4040
    }
}
