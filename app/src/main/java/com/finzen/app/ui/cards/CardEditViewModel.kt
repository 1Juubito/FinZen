package com.finzen.app.ui.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.CreditCardEntity
import com.finzen.app.data.model.CardBrand
import com.finzen.app.data.repository.CreditCardRepository
import com.finzen.app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardEditUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val limitInput: String = "",
    val closingDay: Int = 1,
    val dueDay: Int = 10,
    val colorHex: String = DEFAULT_CARD_COLOR,
    val brand: CardBrand = CardBrand.OTHER,
    val saved: Boolean = false,
) {
    val creditLimit: Double get() = parseLimit(limitInput)
    val canSave: Boolean get() = name.isNotBlank() && creditLimit > 0.0

    companion object {
        const val DEFAULT_CARD_COLOR = "#1B2733"

        fun parseLimit(input: String): Double {
            val cleaned = input
                .filter { it.isDigit() || it == ',' || it == '.' }
                .replace(".", "")
                .replace(',', '.')
            return cleaned.toDoubleOrNull() ?: 0.0
        }
    }
}

class CardEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val creditCardRepository: CreditCardRepository,
) : ViewModel() {

    private val cardId: Long = savedStateHandle.get<Long>(Routes.ARG_CARD_ID) ?: -1L

    private val _state = MutableStateFlow(CardEditUiState(isEditing = cardId > 0L))
    val state = _state.asStateFlow()

    init {
        if (cardId > 0L) loadExisting()
    }

    private fun loadExisting() {
        viewModelScope.launch {
            val card = creditCardRepository.getById(cardId) ?: return@launch
            _state.update {
                it.copy(
                    isEditing = true,
                    name = card.name,
                    limitInput = formatLimit(card.creditLimit),
                    closingDay = card.closingDay.coerceIn(1, 28),
                    dueDay = card.dueDay.coerceIn(1, 28),
                    colorHex = card.colorHex,
                    brand = card.brand,
                )
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setLimit(value: String) = _state.update { it.copy(limitInput = value) }
    fun setClosingDay(value: Int) = _state.update { it.copy(closingDay = value.coerceIn(1, 28)) }
    fun setDueDay(value: Int) = _state.update { it.copy(dueDay = value.coerceIn(1, 28)) }
    fun setColor(hex: String) = _state.update { it.copy(colorHex = hex) }
    fun setBrand(brand: CardBrand) = _state.update { it.copy(brand = brand) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val entity = CreditCardEntity(
                id = if (s.isEditing) cardId else 0L,
                name = s.name.trim(),
                creditLimit = s.creditLimit,
                closingDay = s.closingDay,
                dueDay = s.dueDay,
                colorHex = s.colorHex,
                brand = s.brand,
            )
            if (s.isEditing) {
                creditCardRepository.update(entity)
            } else {
                creditCardRepository.insert(entity)
            }
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        if (cardId <= 0L) return
        viewModelScope.launch {
            creditCardRepository.getById(cardId)?.let { creditCardRepository.delete(it) }
            _state.update { it.copy(saved = true) }
        }
    }

    private fun formatLimit(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
