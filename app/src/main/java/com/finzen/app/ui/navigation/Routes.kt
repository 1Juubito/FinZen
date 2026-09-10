package com.finzen.app.ui.navigation

import com.finzen.app.data.model.TransactionType

object Routes {

    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val CARDS = "cards"
    const val MORE = "more"

    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val BUDGETS = "budgets"
    const val REPORTS = "reports"
    const val GOALS = "goals"
    const val GOAL_PROJECTION = "goal_projection"
    const val INVESTMENTS = "investments"
    const val RECURRING = "recurring"
    const val CATEGORY_RULES = "category_rules"
    const val INSTALLMENTS = "installments"
    const val DEBTS = "debts"
    const val DEBT_PAYOFF = "debt_payoff"
    const val RECEIVABLES = "receivables"
    const val AGENDA = "agenda"
    const val FORECAST = "forecast"
    const val SPENDABLE = "spendable"
    const val BURN_RATE = "burn_rate"
    const val FINANCIAL_HEALTH = "financial_health"
    const val SETTINGS = "settings"

    const val ARG_TX_ID = "txId"
    const val ARG_TX_TYPE = "type"
    const val ARG_CARD_ID = "cardId"
    const val ARG_ACCOUNT_ID = "accountId"
    const val ARG_CATEGORY_ID = "categoryId"
    const val ARG_CATEGORY_TYPE = "categoryType"
    const val ARG_GOAL_ID = "goalId"
    const val ARG_INVESTMENT_ID = "investmentId"

    const val ADD_TRANSACTION = "add_transaction?$ARG_TX_ID={$ARG_TX_ID}&$ARG_TX_TYPE={$ARG_TX_TYPE}&$ARG_CARD_ID={$ARG_CARD_ID}"
    const val ACCOUNT_EDIT = "account_edit?$ARG_ACCOUNT_ID={$ARG_ACCOUNT_ID}"
    const val CARD_EDIT = "card_edit?$ARG_CARD_ID={$ARG_CARD_ID}"
    const val CARD_DETAIL = "card_detail/{$ARG_CARD_ID}"
    const val CATEGORY_EDIT = "category_edit?$ARG_CATEGORY_ID={$ARG_CATEGORY_ID}&$ARG_CATEGORY_TYPE={$ARG_CATEGORY_TYPE}"
    const val GOAL_EDIT = "goal_edit?$ARG_GOAL_ID={$ARG_GOAL_ID}"
    const val INVESTMENT_EDIT = "investment_edit?$ARG_INVESTMENT_ID={$ARG_INVESTMENT_ID}"

    fun addTransaction(
        txId: Long = -1L,
        type: TransactionType = TransactionType.EXPENSE,
        cardId: Long = -1L,
    ): String = "add_transaction?$ARG_TX_ID=$txId&$ARG_TX_TYPE=${type.name}&$ARG_CARD_ID=$cardId"

    fun accountEdit(accountId: Long = -1L): String = "account_edit?$ARG_ACCOUNT_ID=$accountId"
    fun cardEdit(cardId: Long = -1L): String = "card_edit?$ARG_CARD_ID=$cardId"
    fun cardDetail(cardId: Long): String = "card_detail/$cardId"
    fun categoryEdit(categoryId: Long = -1L, type: TransactionType = TransactionType.EXPENSE): String =
        "category_edit?$ARG_CATEGORY_ID=$categoryId&$ARG_CATEGORY_TYPE=${type.name}"
    fun goalEdit(goalId: Long = -1L): String = "goal_edit?$ARG_GOAL_ID=$goalId"
    fun investmentEdit(investmentId: Long = -1L): String =
        "investment_edit?$ARG_INVESTMENT_ID=$investmentId"
}
