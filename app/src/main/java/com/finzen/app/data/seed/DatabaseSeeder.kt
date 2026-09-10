package com.finzen.app.data.seed

import com.finzen.app.data.local.FinanceDatabase
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.local.entity.CreditCardEntity
import com.finzen.app.data.local.entity.InvestmentEntity
import com.finzen.app.data.local.entity.TransactionEntity
import com.finzen.app.data.model.AccountType
import com.finzen.app.data.model.CardBrand
import com.finzen.app.data.model.CategoryType
import com.finzen.app.data.model.InvestmentType
import com.finzen.app.data.model.TransactionType
import com.finzen.app.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate

object DefaultData {

    data class CategorySeed(val name: String, val icon: String, val color: String)

    val expenseCategories = listOf(
        CategorySeed("Alimentação", "restaurant", "#FF924C"),
        CategorySeed("Mercado", "cart", "#82C91E"),
        CategorySeed("Transporte", "car", "#4DABF7"),
        CategorySeed("Moradia", "home", "#A1887F"),
        CategorySeed("Contas e serviços", "bills", "#5C7CFA"),
        CategorySeed("Saúde", "health", "#FF6B6B"),
        CategorySeed("Educação", "education", "#7C5CFC"),
        CategorySeed("Lazer", "leisure", "#E64980"),
        CategorySeed("Compras", "shopping", "#9775FA"),
        CategorySeed("Vestuário", "clothing", "#F06595"),
        CategorySeed("Assinaturas", "subscriptions", "#22B8CF"),
        CategorySeed("Pets", "pets", "#F7B731"),
        CategorySeed("Viagem", "travel", "#12B886"),
        CategorySeed("Outros", "other", "#868E96"),
    )

    val incomeCategories = listOf(
        CategorySeed("Salário", "salary", "#2BB673"),
        CategorySeed("Renda extra", "freelance", "#12B886"),
        CategorySeed("Investimentos", "investments", "#4DABF7"),
        CategorySeed("Vendas", "sales", "#F7B731"),
        CategorySeed("Presentes", "gifts", "#E64980"),
        CategorySeed("Reembolso", "refund", "#82C91E"),
        CategorySeed("Outros", "other", "#868E96"),
    )
}

object DatabaseSeeder {

    suspend fun seedDefaultsIfEmpty(db: FinanceDatabase) {
        if (db.categoryDao().count() == 0) {
            val categories =
                DefaultData.expenseCategories.map {
                    CategoryEntity(
                        name = it.name, type = CategoryType.EXPENSE,
                        colorHex = it.color, iconKey = it.icon, isDefault = true
                    )
                } + DefaultData.incomeCategories.map {
                    CategoryEntity(
                        name = it.name, type = CategoryType.INCOME,
                        colorHex = it.color, iconKey = it.icon, isDefault = true
                    )
                }
            db.categoryDao().insertAll(categories)
        }
        if (db.accountDao().count() == 0) {
            db.accountDao().insert(
                AccountEntity(
                    name = "Carteira", type = AccountType.WALLET,
                    colorHex = "#12B886", iconKey = "wallet", initialBalance = 0.0
                )
            )
            db.accountDao().insert(
                AccountEntity(
                    name = "Conta corrente", type = AccountType.CHECKING,
                    colorHex = "#4DABF7", iconKey = "bank", initialBalance = 0.0
                )
            )
        }
    }

    suspend fun loadSampleData(db: FinanceDatabase) {
        seedDefaultsIfEmpty(db)

        val accounts = db.accountDao().observeActive().first()
        val categories = db.categoryDao().observeAll().first()
        val checking = accounts.firstOrNull { it.type == AccountType.CHECKING } ?: accounts.first()
        val wallet = accounts.firstOrNull { it.type == AccountType.WALLET } ?: accounts.first()
        val byName = categories.associateBy { it.name }

        var cardId = db.creditCardDao().observeAll().first().firstOrNull()?.id
        if (cardId == null) {
            cardId = db.creditCardDao().insert(
                CreditCardEntity(
                    name = "Cartão Black", creditLimit = 8000.0,
                    closingDay = 28, dueDay = 5, colorHex = "#1B2733", brand = CardBrand.MASTERCARD
                )
            )
        }

        val expensePattern = listOf(
            Triple("Mercado", 8, 486.70),
            Triple("Alimentação", 3, 56.90),
            Triple("Alimentação", 16, 92.40),
            Triple("Alimentação", 24, 38.50),
            Triple("Transporte", 10, 140.00),
            Triple("Moradia", 10, 1500.00),
            Triple("Contas e serviços", 12, 189.90),
            Triple("Contas e serviços", 12, 119.90),
            Triple("Lazer", 19, 78.00),
            Triple("Saúde", 21, 210.00),
        )
        val cardPattern = listOf(
            Triple("Compras", 14, 359.90),
            Triple("Assinaturas", 6, 55.80),
        )

        val transactions = mutableListOf<TransactionEntity>()
        for (offset in 0..2) {
            val month = LocalDate.now().minusMonths(offset.toLong())
            fun dateOf(day: Int): Long {
                val safeDay = day.coerceAtMost(month.lengthOfMonth())
                return DateUtils.localDateToMillis(month.withDayOfMonth(safeDay))
            }

            byName["Salário"]?.let { salary ->
                transactions += TransactionEntity(
                    description = "Salário", amount = 5200.0, type = TransactionType.INCOME,
                    date = dateOf(5), categoryId = salary.id, accountId = checking.id, isPaid = true
                )
            }
            expensePattern.forEach { (catName, day, amount) ->
                byName[catName]?.let { cat ->
                    transactions += TransactionEntity(
                        description = catName, amount = amount, type = TransactionType.EXPENSE,
                        date = dateOf(day), categoryId = cat.id, accountId = checking.id, isPaid = true
                    )
                }
            }

            if (offset == 0) {
                cardPattern.forEach { (catName, day, amount) ->
                    byName[catName]?.let { cat ->
                        transactions += TransactionEntity(
                            description = catName, amount = amount, type = TransactionType.EXPENSE,
                            date = dateOf(day), categoryId = cat.id, creditCardId = cardId, isPaid = false
                        )
                    }
                }

                transactions += TransactionEntity(
                    description = "Saque", amount = 300.0, type = TransactionType.TRANSFER,
                    date = dateOf(7), accountId = checking.id, toAccountId = wallet.id, isPaid = true
                )
            }
        }
        db.transactionDao().insertAll(transactions)

        if (db.investmentDao().observeAll().first().isEmpty()) {
            fun invest(
                name: String,
                type: InvestmentType,
                institution: String,
                invested: Double,
                current: Double,
            ) = InvestmentEntity(
                name = name,
                type = type,
                institution = institution,
                investedAmount = invested,
                currentValue = current,
                colorHex = type.colorHex,
            )

            listOf(
                invest("Tesouro Selic 2029", InvestmentType.TREASURY, "NuInvest", 5000.0, 5320.0),
                invest("ITSA4", InvestmentType.STOCKS, "Clear", 3000.0, 3480.0),
                invest("Fundo Multimercado", InvestmentType.FUNDS, "XP", 4000.0, 3860.0),
                invest("Bitcoin", InvestmentType.CRYPTO, "Binance", 2000.0, 2640.0),
            ).forEach { db.investmentDao().upsert(it) }
        }
    }

    suspend fun clearTransactions(db: FinanceDatabase) {
        db.transactionDao().deleteAll()
    }
}
