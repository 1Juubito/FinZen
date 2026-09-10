package com.finzen.app.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.model.AccountWithBalance
import com.finzen.app.data.repository.AccountRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val total: Double = 0.0,
)

class AccountsViewModel(
    accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.observeAccountsWithBalance(),
        accountRepository.observeTotalBalance(),
    ) { accounts, total ->
        AccountsUiState(
            accounts = accounts,
            total = total,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())
}
