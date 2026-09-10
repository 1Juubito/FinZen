package com.finzen.app.data.model

enum class TransactionType(val label: String) {
    INCOME("Receita"),
    EXPENSE("Despesa"),
    TRANSFER("Transferência")
}

enum class AccountType(val label: String) {
    CHECKING("Conta corrente"),
    SAVINGS("Poupança"),
    WALLET("Dinheiro"),
    INVESTMENT("Investimentos"),
    OTHER("Outra")
}

enum class CategoryType { INCOME, EXPENSE }

enum class RecurrenceFrequency(val label: String) {
    DAILY("Diária"),
    WEEKLY("Semanal"),
    MONTHLY("Mensal"),
    YEARLY("Anual"),
}

enum class DebtDirection(val label: String) {
    THEY_OWE_ME("Me devem"),
    I_OWE("Eu devo"),
}

enum class CardBrand(val label: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    ELO("Elo"),
    AMEX("American Express"),
    HIPERCARD("Hipercard"),
    OTHER("Outra")
}

enum class InvestmentType(
    val label: String,
    val colorHex: String,
    val iconKey: String,
) {
    FIXED_INCOME("Renda fixa", "#2BB673", "bank"),
    TREASURY("Tesouro Direto", "#12B886", "savings"),
    STOCKS("Ações", "#4DABF7", "trending_up"),
    FUNDS("Fundos", "#5C7CFA", "chart"),
    REAL_ESTATE("Fundos imobiliários", "#A1887F", "apartment"),
    CRYPTO("Criptomoedas", "#F7B731", "crypto"),
    PENSION("Previdência", "#7C5CFC", "shield"),
    SAVINGS("Poupança", "#22B8CF", "money"),
    OTHER("Outros", "#868E96", "other"),
}
