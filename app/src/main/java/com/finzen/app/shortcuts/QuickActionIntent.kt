package com.finzen.app.shortcuts

import android.content.Context
import android.content.Intent
import com.finzen.app.MainActivity
import com.finzen.app.data.model.TransactionType
import com.finzen.app.ui.navigation.Routes

object QuickActionIntent {

    const val ACTION_ADD_EXPENSE = "com.finzen.app.shortcut.ADD_EXPENSE"
    const val ACTION_ADD_INCOME = "com.finzen.app.shortcut.ADD_INCOME"
    const val ACTION_ADD_TRANSFER = "com.finzen.app.shortcut.ADD_TRANSFER"

    private fun actionFor(type: TransactionType): String = when (type) {
        TransactionType.EXPENSE -> ACTION_ADD_EXPENSE
        TransactionType.INCOME -> ACTION_ADD_INCOME
        TransactionType.TRANSFER -> ACTION_ADD_TRANSFER
    }

    fun build(context: Context, type: TransactionType): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = actionFor(type)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    fun routeFor(intent: Intent?): String? = when (intent?.action) {
        ACTION_ADD_EXPENSE -> Routes.addTransaction(type = TransactionType.EXPENSE)
        ACTION_ADD_INCOME -> Routes.addTransaction(type = TransactionType.INCOME)
        ACTION_ADD_TRANSFER -> Routes.addTransaction(type = TransactionType.TRANSFER)
        else -> null
    }
}
