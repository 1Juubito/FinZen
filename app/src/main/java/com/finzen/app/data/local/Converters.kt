package com.finzen.app.data.local

import androidx.room.TypeConverter
import com.finzen.app.data.model.AccountType
import com.finzen.app.data.model.CardBrand
import com.finzen.app.data.model.CategoryType
import com.finzen.app.data.model.DebtDirection
import com.finzen.app.data.model.InvestmentType
import com.finzen.app.data.model.RecurrenceFrequency
import com.finzen.app.data.model.TransactionType

class Converters {
    @TypeConverter fun transactionTypeToString(v: TransactionType): String = v.name
    @TypeConverter fun stringToTransactionType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun accountTypeToString(v: AccountType): String = v.name
    @TypeConverter fun stringToAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun categoryTypeToString(v: CategoryType): String = v.name
    @TypeConverter fun stringToCategoryType(v: String): CategoryType = CategoryType.valueOf(v)

    @TypeConverter fun cardBrandToString(v: CardBrand): String = v.name
    @TypeConverter fun stringToCardBrand(v: String): CardBrand = CardBrand.valueOf(v)

    @TypeConverter fun investmentTypeToString(v: InvestmentType): String = v.name
    @TypeConverter fun stringToInvestmentType(v: String): InvestmentType = InvestmentType.valueOf(v)

    @TypeConverter fun recurrenceFrequencyToString(v: RecurrenceFrequency): String = v.name
    @TypeConverter fun stringToRecurrenceFrequency(v: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(v)

    @TypeConverter fun debtDirectionToString(v: DebtDirection): String = v.name
    @TypeConverter fun stringToDebtDirection(v: String): DebtDirection = DebtDirection.valueOf(v)
}
