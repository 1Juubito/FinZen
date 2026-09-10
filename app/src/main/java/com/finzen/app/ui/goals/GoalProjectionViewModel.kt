package com.finzen.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finzen.app.data.local.entity.GoalEntity
import com.finzen.app.data.repository.GoalRepository
import com.finzen.app.util.DateUtils
import com.finzen.app.util.Money
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

enum class ProjLevel { GOOD, OK, BAD }

data class GoalProjectionUi(
    val id: Long,
    val name: String,
    val saved: Double,
    val target: Double,
    val remaining: Double,
    val progress: Float,
    val statusLabel: String,
    val level: ProjLevel,
    val headline: String,
    val detail: String?,
)

data class GoalProjectionUiState(
    val items: List<GoalProjectionUi> = emptyList(),
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
    val hasGoals: Boolean = false,
    val loading: Boolean = true,
)

class GoalProjectionViewModel(
    goalRepository: GoalRepository,
) : ViewModel() {

    val uiState: StateFlow<GoalProjectionUiState> = goalRepository.observeAll().map { goals ->
        val today = LocalDate.now()
        GoalProjectionUiState(
            items = goals.map { project(it, today) },
            totalSaved = goals.sumOf { it.savedAmount },
            totalTarget = goals.sumOf { it.targetAmount },
            hasGoals = goals.isNotEmpty(),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalProjectionUiState())

    private fun project(goal: GoalEntity, today: LocalDate): GoalProjectionUi {
        val target = goal.targetAmount
        val saved = goal.savedAmount
        val remaining = (target - saved).coerceAtLeast(0.0)
        val progress = if (target > 0.0) (saved / target).toFloat().coerceIn(0f, 1f) else 1f
        val nowYM = YearMonth.from(today)

        val createdLocal = DateUtils.toLocalDate(goal.createdAt)
        val daysSince = today.toEpochDay() - createdLocal.toEpochDay()
        val monthsSince = (daysSince / 30.4369).coerceAtLeast(1.0)
        val pace = saved / monthsSince
        val tooNew = daysSince < 30

        val statusLabel: String
        val level: ProjLevel
        val headline: String
        val detail: String?

        val deadline = goal.deadline
        when {
            remaining <= 0.0 -> {
                statusLabel = "Concluída"; level = ProjLevel.GOOD
                headline = "Meta concluída 🎉"
                detail = "Você guardou ${Money.format(saved)}"
            }
            deadline != null -> {
                val dueLocal = DateUtils.toLocalDate(deadline)
                val dueYM = YearMonth.from(dueLocal)
                val dueLabel = "${DateUtils.shortMonthLabel(dueYM.year, dueYM.monthValue)}/${dueYM.year}"
                if (dueLocal.toEpochDay() < today.toEpochDay()) {
                    statusLabel = "Prazo vencido"; level = ProjLevel.BAD
                    headline = "Prazo venceu em $dueLabel"
                    detail = "Faltam ${Money.format(remaining)}"
                } else {
                    val monthsLeft = ((dueYM.year - nowYM.year) * 12 + (dueYM.monthValue - nowYM.monthValue)).coerceAtLeast(1)
                    val required = remaining / monthsLeft
                    val onTrack = pace >= required
                    statusLabel = if (onTrack) "No ritmo" else "Acelerar"
                    level = if (onTrack) ProjLevel.GOOD else ProjLevel.OK
                    headline = "Guarde ${Money.format(required)}/mês até $dueLabel"
                    detail = "Faltam ${Money.format(remaining)} · $monthsLeft ${if (monthsLeft == 1) "mês" else "meses"}"
                }
            }
            pace <= 0.0 -> {
                statusLabel = "Sem aportes"; level = ProjLevel.OK
                headline = "Comece a guardar para projetar"
                detail = "Faltam ${Money.format(remaining)}"
            }
            tooNew -> {
                statusLabel = "Recente"; level = ProjLevel.OK
                headline = "Guardando há pouco tempo"
                detail = "Faltam ${Money.format(remaining)} · a projeção fica mais precisa com o tempo"
            }
            else -> {
                val months = ceil(remaining / pace).toInt()
                if (months > 120) {
                    statusLabel = "Muito tempo"; level = ProjLevel.OK
                    headline = "Faltam ${Money.format(remaining)}"
                    detail = "No ritmo atual (~${Money.format(pace)}/mês) levaria mais de 10 anos"
                } else {
                    val etaYM = nowYM.plusMonths(months.toLong())
                    val etaLabel = "${DateUtils.shortMonthLabel(etaYM.year, etaYM.monthValue)}/${etaYM.year}"
                    statusLabel = "Em progresso"; level = ProjLevel.OK
                    headline = "Chega em ~$months ${if (months == 1) "mês" else "meses"} ($etaLabel)"
                    detail = "No seu ritmo de ~${Money.format(pace)}/mês · faltam ${Money.format(remaining)}"
                }
            }
        }

        return GoalProjectionUi(goal.id, goal.name, saved, target, remaining, progress, statusLabel, level, headline, detail)
    }
}
