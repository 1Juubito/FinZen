package com.finzen.app.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.AccountEntity
import com.finzen.app.data.model.AccountType
import com.finzen.app.data.repository.AccountRepository
import com.finzen.app.ui.icons.PaletteColors
import com.finzen.app.ui.navigation.Routes
import com.finzen.app.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountEditUiState(
    val name: String = "",
    val type: AccountType = AccountType.CHECKING,
    val initialBalanceDigits: String = "",
    val colorHex: String = PaletteColors.swatches.first(),
    val iconKey: String = "wallet",
    val includeInTotal: Boolean = true,
    val isEditing: Boolean = false,
    val saved: Boolean = false,
) {
    val initialBalance: Double get() = Money.centsToValue(initialBalanceDigits)
    val initialBalanceFormatted: String get() = Money.formatCentsInput(initialBalanceDigits)
    val canSave: Boolean get() = name.isNotBlank()
}

class AccountEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val accountId: Long = savedStateHandle.get<Long>(Routes.ARG_ACCOUNT_ID) ?: -1L

    private val _state = MutableStateFlow(AccountEditUiState(isEditing = accountId > 0L))
    val state = _state.asStateFlow()

    init {
        if (accountId > 0L) loadExisting()
    }

    private fun loadExisting() {
        viewModelScope.launch {
            val account = accountRepository.getById(accountId) ?: return@launch
            _state.update {
                it.copy(
                    name = account.name,
                    type = account.type,
                    initialBalanceDigits = toDigits(account.initialBalance),
                    colorHex = account.colorHex,
                    iconKey = account.iconKey,
                    includeInTotal = account.includeInTotal,
                    isEditing = true,
                )
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun setType(type: AccountType) = _state.update { it.copy(type = type) }

    fun appendDigit(digit: Char) {
        if (!digit.isDigit()) return
        _state.update {
            val next = (it.initialBalanceDigits + digit).trimStart('0').take(12)
            it.copy(initialBalanceDigits = next)
        }
    }

    fun backspace() {
        _state.update { it.copy(initialBalanceDigits = it.initialBalanceDigits.dropLast(1)) }
    }

    fun selectColor(hex: String) = _state.update { it.copy(colorHex = hex) }

    fun selectIcon(key: String) = _state.update { it.copy(iconKey = key) }

    fun toggleIncludeInTotal() = _state.update { it.copy(includeInTotal = !it.includeInTotal) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val entity = AccountEntity(
                id = if (s.isEditing) accountId else 0L,
                name = s.name.trim(),
                type = s.type,
                initialBalance = s.initialBalance,
                colorHex = s.colorHex,
                iconKey = s.iconKey,
                includeInTotal = s.includeInTotal,
            )
            if (s.isEditing) {
                accountRepository.update(entity)
            } else {
                accountRepository.insert(entity)
            }
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        if (accountId <= 0L) return
        viewModelScope.launch {
            accountRepository.getById(accountId)?.let { accountRepository.delete(it) }
            _state.update { it.copy(saved = true) }
        }
    }

    private fun toDigits(amount: Double): String {
        val cents = Math.round(amount * 100.0)
        return if (cents <= 0) "" else cents.toString()
    }
}
