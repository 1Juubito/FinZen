package com.finzen.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finzen.app.data.local.dao.AccountDao
import com.finzen.app.data.local.dao.BudgetDao
import com.finzen.app.data.local.dao.CategoryDao
import com.finzen.app.data.local.dao.CategoryRuleDao
import com.finzen.app.data.local.dao.CreditCardDao
import com.finzen.app.data.local.dao.GoalDao
import com.finzen.app.data.local.dao.InvestmentDao
import com.finzen.app.data.local.dao.PersonalDebtDao
import com.finzen.app.data.local.dao.RecurringTransactionDao
import com.finzen.app.data.local.dao.TransactionDao
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

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        CreditCardEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        InvestmentEntity::class,
        RecurringTransactionEntity::class,
        CategoryRuleEntity::class,
        PersonalDebtEntity::class,
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun personalDebtDao(): PersonalDebtDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `investments` (" +
                        "`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                        "`institution` TEXT NOT NULL, `investedAmount` REAL NOT NULL, " +
                        "`currentValue` REAL NOT NULL, `date` INTEGER, `colorHex` TEXT NOT NULL, " +
                        "`notes` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurring_transactions` (" +
                        "`id` INTEGER NOT NULL, `description` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                        "`type` TEXT NOT NULL, `categoryId` INTEGER, `accountId` INTEGER, " +
                        "`creditCardId` INTEGER, `notes` TEXT, `frequency` TEXT NOT NULL, " +
                        "`interval` INTEGER NOT NULL, `startDate` INTEGER NOT NULL, `endDate` INTEGER, " +
                        "`nextDueDate` INTEGER NOT NULL, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, " +
                        "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, " +
                        "FOREIGN KEY(`creditCardId`) REFERENCES `credit_cards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_categoryId` ON `recurring_transactions` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_accountId` ON `recurring_transactions` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_creditCardId` ON `recurring_transactions` (`creditCardId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_rules` (" +
                        "`id` INTEGER NOT NULL, `keyword` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_rules_categoryId` ON `category_rules` (`categoryId`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `invoiceDate` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `transactions` SET `invoiceDate` = `date`")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `personal_debts` (" +
                        "`id` INTEGER NOT NULL, `person` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                        "`direction` TEXT NOT NULL, `date` INTEGER NOT NULL, `dueDate` INTEGER, " +
                        "`notes` TEXT, `settled` INTEGER NOT NULL, `settledAt` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        fun get(context: Context): FinanceDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finzen.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
