package com.finzen.app.ui.investments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.local.entity.InvestmentEntity
import com.finzen.app.data.local.entity.TransactionEntity
import com.finzen.app.data.model.InvestmentType
import com.finzen.app.data.model.TransactionType
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.data.repository.InvestmentRepository
import com.finzen.app.data.repository.TransactionRepository
import com.finzen.app.ui.navigation.Routes
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AmountField { INVESTED, CURRENT }

data class InvestmentEditUiState(
    val name: String = "",
    val type: InvestmentType = InvestmentType.FIXED_INCOME,
    val institution: String = "",
    val investedDigits: String = "",
    val currentDigits: String = "",
    val editingField: AmountField = AmountField.INVESTED,
    val date: Long? = null,
    val colorHex: String = InvestmentType.FIXED_INCOME.colorHex,
    val notes: String = "",
    val isEditing: Boolean = false,
    val saved: Boolean = false,
    val accounts: List<AccountEntity> = emptyList(),
    val sourceAccountId: Long? = null,
) {
    val investedAmount: Double get() = Money.centsToValue(investedDigits)
    val currentValue: Double get() = Money.centsToValue(currentDigits)
    val investedFormatted: String get() = Money.formatCentsInput(investedDigits)
    val currentFormatted: String get() = Money.formatCentsInput(currentDigits)
    val dateLabel: String get() = date?.let { DateUtils.fullDateLabel(it) } ?: "Sem data"
    val canSave: Boolean get() = name.isNotBlank() && investedAmount > 0.0
}

class InvestmentEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val investmentRepository: InvestmentRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val investmentId: Long = savedStateHandle.get<Long>(Routes.ARG_INVESTMENT_ID) ?: -1L

    private var createdAt: Long = DateUtils.now()
    private var colorCustomized = false

    private var currentEdited = false
    private var sourceTouched = false

    private val _state = MutableStateFlow(InvestmentEditUiState(isEditing = investmentId > 0L))
    val state = _state.asStateFlow()

    init {
        if (investmentId > 0L) loadExisting()

        accountRepository.observeActive()
            .onEach { accounts ->
                _state.update { s ->
                    val source = if (sourceTouched) s.sourceAccountId
                        else accounts.firstOrNull { it.includeInTotal }?.id ?: accounts.firstOrNull()?.id
                    s.copy(accounts = accounts, sourceAccountId = source)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadExisting() {
        viewModelScope.launch {
            val inv = investmentRepository.observeById(investmentId).first() ?: return@launch
            createdAt = inv.createdAt
            colorCustomized = true
            currentEdited = true
            _state.update {
                it.copy(
                    name = inv.name,
                    type = inv.type,
                    institution = inv.institution,
                    investedDigits = toDigits(inv.investedAmount),
                    currentDigits = toDigits(inv.currentValue),
                    date = inv.date,
                    colorHex = inv.colorHex,
                    notes = inv.notes.orEmpty(),
                    isEditing = true,
                )
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setInstitution(value: String) = _state.update { it.copy(institution = value) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }
    fun setDate(millis: Long?) = _state.update { it.copy(date = millis) }
    fun clearDate() = _state.update { it.copy(date = null) }
    fun setSourceAccount(id: Long?) {
        sourceTouched = true
        _state.update { it.copy(sourceAccountId = id) }
    }

    fun setType(type: InvestmentType) {
        _state.update {
            it.copy(
                type = type,
                colorHex = if (colorCustomized) it.colorHex else type.colorHex,
            )
        }
    }

    fun selectColor(hex: String) {
        colorCustomized = true
        _state.update { it.copy(colorHex = hex) }
    }

    fun selectField(field: AmountField) = _state.update { it.copy(editingField = field) }

    fun appendDigit(digit: Char) {
        if (!digit.isDigit()) return
        _state.update { s ->
            when (s.editingField) {
                AmountField.INVESTED -> {
                    val next = (s.investedDigits + digit).trimStart('0').take(12)
                    if (currentEdited) s.copy(investedDigits = next)
                    else s.copy(investedDigits = next, currentDigits = next)
                }
                AmountField.CURRENT -> {
                    currentEdited = true
                    val next = (s.currentDigits + digit).trimStart('0').take(12)
                    s.copy(currentDigits = next)
                }
            }
        }
    }

    fun backspace() {
        _state.update { s ->
            when (s.editingField) {
                AmountField.INVESTED -> {
                    val next = s.investedDigits.dropLast(1)
                    if (currentEdited) s.copy(investedDigits = next)
                    else s.copy(investedDigits = next, currentDigits = next)
                }
                AmountField.CURRENT -> {
                    currentEdited = true
                    s.copy(currentDigits = s.currentDigits.dropLast(1))
                }
            }
        }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val entity = InvestmentEntity(
                id = if (s.isEditing) investmentId else 0L,
                name = s.name.trim(),
                type = s.type,
                institution = s.institution.trim(),
                investedAmount = s.investedAmount,
                currentValue = if (s.currentDigits.isBlank()) s.investedAmount else s.currentValue,
                date = s.date,
                colorHex = s.colorHex,
                notes = s.notes.ifBlank { null },
                createdAt = createdAt,
            )
            investmentRepository.upsert(entity)

            if (!s.isEditing && s.sourceAccountId != null) {
                transactionRepository.insert(
                    TransactionEntity(
                        description = "Aporte • ${s.name.trim()}",
                        amount = s.investedAmount,
                        type = TransactionType.TRANSFER,
                        date = DateUtils.now(),
                        accountId = s.sourceAccountId,
                        toAccountId = null,
                        isPaid = true,
                    ),
                )
            }
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        if (investmentId <= 0L) return
        val s = _state.value
        viewModelScope.launch {
            val entity = InvestmentEntity(
                id = investmentId,
                name = s.name.trim(),
                type = s.type,
                institution = s.institution.trim(),
                investedAmount = s.investedAmount,
                currentValue = s.currentValue,
                date = s.date,
                colorHex = s.colorHex,
                notes = s.notes.ifBlank { null },
                createdAt = createdAt,
            )
            investmentRepository.delete(entity)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun toDigits(amount: Double): String {
        val cents = Math.round(amount * 100.0)
        return if (cents <= 0) "" else cents.toString()
    }
}
