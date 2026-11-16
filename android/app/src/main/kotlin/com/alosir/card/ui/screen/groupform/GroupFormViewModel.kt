package com.alosir.card.ui.screen.groupform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.DateEngine
import com.alosir.card.domain.model.BillingGroup
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.domain.util.DateProvider
import com.alosir.card.domain.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupFormUiState(
    val isEditing: Boolean = false,
    val groupId: Long = -1L,
    val name: String = "",
    val nameError: String? = null,
    val billingDayText: String = "",
    val billingDayError: String? = null,
    val dueMode: String = DateEngine.DUE_MODE_OFFSET,
    val dueOffsetDays: Int = 20,
    val fixedDayText: String = "25",
    val dueError: String? = null,
    val creditLimitText: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val duePreview: String?
        get() {
            val day = billingDayText.trim().toIntOrNull() ?: return null
            val today = DateProvider.today()
            if (dueMode == DateEngine.DUE_MODE_FIXED_DAY) {
                val fixedDay = fixedDayText.trim().toIntOrNull() ?: return "请输入 1–31 的还款日"
                if (fixedDay !in 1..31) return "还款日须为 1–31 的整数"
                val due = DateEngine.dueDateOf(day, null, dueMode, fixedDay, today)
                return "每月 $fixedDay 号还款 → 本期 ${due.monthValue} 月 ${due.dayOfMonth} 日"
            }
            val due = DateEngine.dueDateOf(day, dueOffsetDays, today)
            return "账单日 $day 号 → 本期还款日 ${due.monthValue} 月 ${due.dayOfMonth} 日"
        }
}

@HiltViewModel
class GroupFormViewModel @Inject constructor(
    private val groupRepository: BillingGroupRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(GroupFormUiState())
    val ui: StateFlow<GroupFormUiState> = _ui.asStateFlow()

    fun load(groupId: Long) {
        if (_ui.value.isEditing || groupId <= 0) return
        viewModelScope.launch {
            val group = groupRepository.observeGroups().first()
                .firstOrNull { it.id == groupId } ?: return@launch
            _ui.update {
                it.copy(
                    isEditing = true,
                    groupId = group.id,
                    name = group.name,
                    billingDayText = group.billingDay?.toString() ?: "",
                    dueMode = group.dueMode,
                    dueOffsetDays = group.dueOffsetDays ?: 20,
                    fixedDayText = group.dueDayOfMonth?.toString() ?: "25",
                    creditLimitText = group.creditLimitCents?.let { c -> Money.format(c).removePrefix("¥") } ?: "",
                )
            }
        }
    }

    fun onNameChange(value: String) =
        _ui.update { it.copy(name = value, nameError = if (value.isBlank()) "名称不能为空" else null) }

    fun onBillingDayChange(value: String) {
        val day = value.trim().toIntOrNull()
        val error = when {
            value.trim().isEmpty() -> null
            day == null || day !in 1..31 -> "账单日须为 1–31 的整数"
            else -> null
        }
        _ui.update { it.copy(billingDayText = value, billingDayError = error) }
    }

    fun onDueModeChange(mode: String) =
        _ui.update { it.copy(dueMode = mode, dueError = null) }

    fun onDueOffsetChange(offset: Int) =
        _ui.update { it.copy(dueOffsetDays = offset.coerceIn(1, 28), dueMode = DateEngine.DUE_MODE_OFFSET) }

    fun onFixedDayChange(value: String) {
        val filtered = value.filter(Char::isDigit).take(2)
        val day = filtered.toIntOrNull()
        val error = if (filtered.isNotEmpty() && (day == null || day !in 1..31)) {
            "还款日须为 1–31 的整数"
        } else {
            null
        }
        _ui.update { it.copy(fixedDayText = filtered, dueError = error, dueMode = DateEngine.DUE_MODE_FIXED_DAY) }
    }

    fun onCreditLimitChange(value: String) = _ui.update { it.copy(creditLimitText = value) }

    fun save() {
        val state = _ui.value
        if (state.saving) return
        if (state.name.isBlank()) {
            _ui.update { it.copy(nameError = "名称不能为空") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(saving = true) }
            val day = state.billingDayText.trim().toIntOrNull()
            if (state.billingDayText.isNotBlank() && (day == null || day !in 1..31)) {
                _ui.update { it.copy(saving = false, billingDayError = "账单日须为 1–31 的整数") }
                return@launch
            }
            val fixedDay = state.fixedDayText.trim().toIntOrNull()
            if (day != null && state.dueMode == DateEngine.DUE_MODE_FIXED_DAY &&
                (fixedDay == null || fixedDay !in 1..31)
            ) {
                _ui.update { it.copy(saving = false, dueError = "还款日须为 1–31 的整数") }
                return@launch
            }
            val group = BillingGroup(
                id = if (state.isEditing) state.groupId else 0L,
                name = state.name.trim(),
                billingDay = day,
                dueOffsetDays = if (day != null && state.dueMode == DateEngine.DUE_MODE_OFFSET) {
                    state.dueOffsetDays
                } else {
                    null
                },
                dueMode = state.dueMode,
                dueDayOfMonth = if (day != null && state.dueMode == DateEngine.DUE_MODE_FIXED_DAY) {
                    fixedDay
                } else {
                    null
                },
                creditLimitCents = Money.parseToCents(state.creditLimitText),
                bankName = state.name.trim().ifEmpty { null },
            )
            if (state.isEditing) {
                groupRepository.updateGroup(group)
            } else {
                groupRepository.createGroup(group)
            }
            _ui.update { it.copy(saving = false, saved = true) }
        }
    }

    fun delete() {
        val id = _ui.value.groupId
        if (id <= 0) return
        viewModelScope.launch {
            groupRepository.deleteGroup(id)
            _ui.update { it.copy(deleted = true) }
        }
    }
}
