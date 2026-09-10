package com.finzen.app.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.model.CardWithUsage
import com.finzen.app.data.repository.CreditCardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CardsUiState(
    val cards: List<CardWithUsage> = emptyList(),
    val totalOpen: Double = 0.0,
    val totalLimit: Double = 0.0,
)

class CardsViewModel(
    creditCardRepository: CreditCardRepository,
) : ViewModel() {

    val uiState: StateFlow<CardsUiState> = creditCardRepository.observeCardsWithUsage()
        .map { cards ->
            CardsUiState(
                cards = cards,
                totalOpen = cards.sumOf { it.used },
                totalLimit = cards.sumOf { it.card.creditLimit },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardsUiState())
}
