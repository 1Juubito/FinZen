package com.finzen.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.finzen.app.FinanceApp
import com.finzen.app.MainActivity
import com.finzen.app.R
import com.finzen.app.data.model.TransactionType
import com.finzen.app.shortcuts.QuickActionIntent
import com.finzen.app.util.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = context.applicationContext as FinanceApp

        val pending = goAsync()
        app.applicationScope.launch {
            try {
                val (balance, visible) = loadBalance(app)
                appWidgetIds.forEach { id -> render(context, appWidgetManager, id, balance, visible) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        fun refresh(context: Context) {
            val app = context.applicationContext as? FinanceApp ?: return
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, BalanceWidgetProvider::class.java))
            if (ids.isEmpty()) return
            app.applicationScope.launch {
                val (balance, visible) = loadBalance(app)
                ids.forEach { id -> render(context, manager, id, balance, visible) }
            }
        }

        private suspend fun loadBalance(app: FinanceApp): Pair<Double, Boolean> {
            val balance = app.container.accountRepository.observeTotalBalance().first()
            val visible = app.container.settingsRepository.balanceVisible.first()
            return balance to visible
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            balance: Double,
            visible: Boolean,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_balance).apply {
                setTextViewText(R.id.widget_balance, if (visible) Money.format(balance) else "R$ ••••••")
                setOnClickPendingIntent(R.id.widget_root, openApp(context))
                setOnClickPendingIntent(R.id.widget_btn_expense, addTransaction(context, TransactionType.EXPENSE))
                setOnClickPendingIntent(R.id.widget_btn_income, addTransaction(context, TransactionType.INCOME))
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun pendingFlags(): Int =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        private fun openApp(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(context, 100, intent, pendingFlags())
        }

        private fun addTransaction(context: Context, type: TransactionType): PendingIntent {
            val requestCode = when (type) {
                TransactionType.EXPENSE -> 101
                TransactionType.INCOME -> 102
                TransactionType.TRANSFER -> 103
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                QuickActionIntent.build(context, type),
                pendingFlags(),
            )
        }
    }
}
