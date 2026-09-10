package com.finzen.app.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import com.finzen.app.data.local.FinanceDatabase
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.local.entity.BudgetEntity
import com.finzen.app.data.local.entity.CategoryEntity
import com.finzen.app.data.local.entity.CategoryRuleEntity
import com.finzen.app.data.local.entity.CreditCardEntity
import com.finzen.app.data.local.entity.GoalEntity
import com.finzen.app.data.local.entity.InvestmentEntity
import com.finzen.app.data.local.entity.PersonalDebtEntity
import com.finzen.app.data.local.entity.RecurringTransactionEntity
import com.finzen.app.data.local.entity.TransactionEntity
import com.finzen.app.data.model.AccountType
import com.finzen.app.data.model.CardBrand
import com.finzen.app.data.model.CategoryType
import com.finzen.app.data.model.DebtDirection
import com.finzen.app.data.model.InvestmentType
import com.finzen.app.data.model.RecurrenceFrequency
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(
    private val context: Context,
    private val db: FinanceDatabase,
    private val settings: SettingsRepository,
) {

    private suspend fun buildRoot(): JSONObject = JSONObject().apply {
        put("app", "Error404Saldo")
        put("schema", SCHEMA)
        put("exportedAt", System.currentTimeMillis())
        put("accounts", db.accountDao().getAll().toArray { it.toJson() })
        put("categories", db.categoryDao().getAll().toArray { it.toJson() })
        put("creditCards", db.creditCardDao().getAll().toArray { it.toJson() })
        put("transactions", db.transactionDao().getAll().toArray { it.toJson() })
        put("budgets", db.budgetDao().getAll().toArray { it.toJson() })
        put("goals", db.goalDao().getAll().toArray { it.toJson() })
        put("investments", db.investmentDao().getAll().toArray { it.toJson() })
        put("recurringTransactions", db.recurringTransactionDao().getAll().toArray { it.toJson() })
        put("categoryRules", db.categoryRuleDao().getAll().toArray { it.toJson() })
        put("personalDebts", db.personalDebtDao().getAll().toArray { it.toJson() })
    }

    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val root = buildRoot()
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Não foi possível abrir o arquivo para escrita.")
        out.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
        root.optJSONArray("transactions")?.length() ?: 0
    }

    suspend fun runAutoBackupIfDue(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!settings.autoBackupEnabled.first()) return@withContext false
        val folder = settings.autoBackupFolder.first()?.takeIf { it.isNotBlank() }
            ?: return@withContext false
        val now = System.currentTimeMillis()
        if (!force && now - settings.lastAutoBackupAt.first() < MIN_INTERVAL_MS) return@withContext false

        val treeUri = Uri.parse(folder)
        val text = buildRoot().toString(2)
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale("pt", "BR")).format(Date(now))
        writeDocument(treeUri, "$AUTO_PREFIX$stamp.json", text)
        runCatching { rotate(treeUri, KEEP) }
        settings.setLastAutoBackupAt(now)
        true
    }

    private fun writeDocument(treeUri: Uri, fileName: String, text: String) {
        val cr = context.contentResolver
        val dirUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri, DocumentsContract.getTreeDocumentId(treeUri),
        )
        val fileUri = DocumentsContract.createDocument(cr, dirUri, "application/json", fileName)
            ?: throw IllegalStateException("Não foi possível criar o arquivo na pasta de backup.")
        cr.openOutputStream(fileUri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("Não foi possível escrever o backup.")
    }

    private fun rotate(treeUri: Uri, keep: Int) {
        val cr = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
        val files = mutableListOf<Pair<String, String>>()
        cr.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                if (id != null && name != null && name.startsWith(AUTO_PREFIX) && name.endsWith(".json")) {
                    files += id to name
                }
            }
        }
        if (files.size <= keep) return

        files.sortedBy { it.second }.dropLast(keep).forEach { (id, _) ->
            runCatching {
                DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(treeUri, id))
            }
        }
    }

    suspend fun import(uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw IllegalStateException("Não foi possível ler o arquivo.")

        val root = JSONObject(text)
        val accounts = root.optJSONArray("accounts").mapObjects { it.toAccount() }
        val categories = root.optJSONArray("categories").mapObjects { it.toCategory() }
        val cards = root.optJSONArray("creditCards").mapObjects { it.toCard() }
        val transactions = root.optJSONArray("transactions").mapObjects { it.toTransaction() }
        val budgets = root.optJSONArray("budgets").mapObjects { it.toBudget() }
        val goals = root.optJSONArray("goals").mapObjects { it.toGoal() }
        val investments = root.optJSONArray("investments").mapObjects { it.toInvestment() }
        val recurrings = root.optJSONArray("recurringTransactions").mapObjects { it.toRecurring() }
        val rules = root.optJSONArray("categoryRules").mapObjects { it.toRule() }
        val debts = root.optJSONArray("personalDebts").mapObjects { it.toPersonalDebt() }

        db.withTransaction {

            db.transactionDao().deleteAll()
            db.budgetDao().deleteAll()
            db.goalDao().deleteAll()
            db.investmentDao().deleteAll()
            db.recurringTransactionDao().deleteAll()
            db.categoryRuleDao().deleteAll()
            db.personalDebtDao().deleteAll()
            db.creditCardDao().deleteAll()
            db.accountDao().deleteAll()
            db.categoryDao().deleteAll()

            db.categoryDao().insertAll(categories)
            db.accountDao().insertAll(accounts)
            db.creditCardDao().insertAll(cards)
            db.transactionDao().insertAll(transactions)
            db.budgetDao().insertAll(budgets)
            db.goalDao().insertAll(goals)
            db.investmentDao().insertAll(investments)
            db.recurringTransactionDao().insertAll(recurrings)
            db.categoryRuleDao().insertAll(rules)
            db.personalDebtDao().insertAll(debts)
        }
        transactions.size
    }

    private companion object {
        const val SCHEMA = 1
        const val AUTO_PREFIX = "error404saldo-auto-"
        const val KEEP = 14
        const val MIN_INTERVAL_MS = 12L * 60 * 60 * 1000
    }
}

private fun AccountEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("type", type.name)
    put("initialBalance", initialBalance); put("colorHex", colorHex); put("iconKey", iconKey)
    put("includeInTotal", includeInTotal); put("archived", archived)
}

private fun CategoryEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("type", type.name)
    put("colorHex", colorHex); put("iconKey", iconKey); put("isDefault", isDefault)
}

private fun CreditCardEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("creditLimit", creditLimit)
    put("closingDay", closingDay); put("dueDay", dueDay); put("colorHex", colorHex); put("brand", brand.name)
}

private fun TransactionEntity.toJson() = JSONObject().apply {
    put("id", id); put("description", description); put("amount", amount); put("type", type.name)
    put("date", date); put("invoiceDate", invoiceDate); put("categoryId", categoryId); put("accountId", accountId)
    put("toAccountId", toAccountId); put("creditCardId", creditCardId); put("isPaid", isPaid)
    put("notes", notes); put("installmentNumber", installmentNumber)
    put("installmentTotal", installmentTotal); put("groupId", groupId); put("createdAt", createdAt)
}

private fun BudgetEntity.toJson() = JSONObject().apply {
    put("id", id); put("categoryId", categoryId); put("amount", amount); put("year", year); put("month", month)
}

private fun GoalEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("targetAmount", targetAmount); put("savedAmount", savedAmount)
    put("deadline", deadline); put("colorHex", colorHex); put("iconKey", iconKey); put("createdAt", createdAt)
}

private fun InvestmentEntity.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("type", type.name); put("institution", institution)
    put("investedAmount", investedAmount); put("currentValue", currentValue); put("date", date)
    put("colorHex", colorHex); put("notes", notes); put("createdAt", createdAt)
}

private fun RecurringTransactionEntity.toJson() = JSONObject().apply {
    put("id", id); put("description", description); put("amount", amount); put("type", type.name)
    put("categoryId", categoryId); put("accountId", accountId); put("creditCardId", creditCardId)
    put("notes", notes); put("frequency", frequency.name); put("interval", interval)
    put("startDate", startDate); put("endDate", endDate); put("nextDueDate", nextDueDate)
    put("active", active); put("createdAt", createdAt)
}

private fun CategoryRuleEntity.toJson() = JSONObject().apply {
    put("id", id); put("keyword", keyword); put("categoryId", categoryId); put("createdAt", createdAt)
}

private fun PersonalDebtEntity.toJson() = JSONObject().apply {
    put("id", id); put("person", person); put("amount", amount); put("direction", direction.name)
    put("date", date); put("dueDate", dueDate); put("notes", notes)
    put("settled", settled); put("settledAt", settledAt); put("createdAt", createdAt)
}

private fun JSONObject.toAccount() = AccountEntity(
    id = optLong("id"),
    name = optString("name"),
    type = enumOr(optString("type"), AccountType.OTHER),
    initialBalance = optDouble("initialBalance", 0.0),
    colorHex = optString("colorHex", "#868E96"),
    iconKey = optString("iconKey", "wallet"),
    includeInTotal = optBoolean("includeInTotal", true),
    archived = optBoolean("archived", false),
)

private fun JSONObject.toCategory() = CategoryEntity(
    id = optLong("id"),
    name = optString("name"),
    type = enumOr(optString("type"), CategoryType.EXPENSE),
    colorHex = optString("colorHex", "#868E96"),
    iconKey = optString("iconKey", "other"),
    isDefault = optBoolean("isDefault", false),
)

private fun JSONObject.toCard() = CreditCardEntity(
    id = optLong("id"),
    name = optString("name"),
    creditLimit = optDouble("creditLimit", 0.0),
    closingDay = optInt("closingDay", 1),
    dueDay = optInt("dueDay", 10),
    colorHex = optString("colorHex", "#1B2733"),
    brand = enumOr(optString("brand"), CardBrand.OTHER),
)

private fun JSONObject.toTransaction() = TransactionEntity(
    id = optLong("id"),
    description = optString("description"),
    amount = optDouble("amount", 0.0),
    type = enumOr(optString("type"), TransactionType.EXPENSE),
    date = optLong("date"),
    invoiceDate = longOrNull("invoiceDate") ?: optLong("date"),
    categoryId = longOrNull("categoryId"),
    accountId = longOrNull("accountId"),
    toAccountId = longOrNull("toAccountId"),
    creditCardId = longOrNull("creditCardId"),
    isPaid = optBoolean("isPaid", true),
    notes = stringOrNull("notes"),
    installmentNumber = intOrNull("installmentNumber"),
    installmentTotal = intOrNull("installmentTotal"),
    groupId = stringOrNull("groupId"),
    createdAt = optLong("createdAt"),
)

private fun JSONObject.toBudget() = BudgetEntity(
    id = optLong("id"),
    categoryId = optLong("categoryId"),
    amount = optDouble("amount", 0.0),
    year = optInt("year", 0),
    month = optInt("month", 0),
)

private fun JSONObject.toGoal() = GoalEntity(
    id = optLong("id"),
    name = optString("name"),
    targetAmount = optDouble("targetAmount", 0.0),
    savedAmount = optDouble("savedAmount", 0.0),
    deadline = longOrNull("deadline"),
    colorHex = optString("colorHex", "#868E96"),
    iconKey = optString("iconKey", "flag"),
    createdAt = optLong("createdAt"),
)

private fun JSONObject.toInvestment() = InvestmentEntity(
    id = optLong("id"),
    name = optString("name"),
    type = enumOr(optString("type"), InvestmentType.OTHER),
    institution = optString("institution", ""),
    investedAmount = optDouble("investedAmount", 0.0),
    currentValue = optDouble("currentValue", 0.0),
    date = longOrNull("date"),
    colorHex = optString("colorHex", "#868E96"),
    notes = stringOrNull("notes"),
    createdAt = optLong("createdAt"),
)

private fun JSONObject.toRecurring() = RecurringTransactionEntity(
    id = optLong("id"),
    description = optString("description"),
    amount = optDouble("amount", 0.0),
    type = enumOr(optString("type"), TransactionType.EXPENSE),
    categoryId = longOrNull("categoryId"),
    accountId = longOrNull("accountId"),
    creditCardId = longOrNull("creditCardId"),
    notes = stringOrNull("notes"),
    frequency = enumOr(optString("frequency"), RecurrenceFrequency.MONTHLY),
    interval = optInt("interval", 1),
    startDate = optLong("startDate"),
    endDate = longOrNull("endDate"),
    nextDueDate = optLong("nextDueDate"),
    active = optBoolean("active", true),
    createdAt = optLong("createdAt"),
)

private fun JSONObject.toRule() = CategoryRuleEntity(
    id = optLong("id"),
    keyword = optString("keyword"),
    categoryId = optLong("categoryId"),
    createdAt = optLong("createdAt"),
)

private fun JSONObject.toPersonalDebt() = PersonalDebtEntity(
    id = optLong("id"),
    person = optString("person"),
    amount = optDouble("amount", 0.0),
    direction = enumOr(optString("direction"), DebtDirection.THEY_OWE_ME),
    date = optLong("date"),
    dueDate = longOrNull("dueDate"),
    notes = stringOrNull("notes"),
    settled = optBoolean("settled", false),
    settledAt = longOrNull("settledAt"),
    createdAt = optLong("createdAt"),
)

private inline fun <T> List<T>.toArray(transform: (T) -> JSONObject): JSONArray =
    JSONArray().also { arr -> forEach { arr.put(transform(it)) } }

private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    val arr = this ?: return emptyList()
    return (0 until arr.length()).map { transform(arr.getJSONObject(it)) }
}

private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else optInt(key)
private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else optString(key)

private inline fun <reified T : Enum<T>> enumOr(name: String, default: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(default)
