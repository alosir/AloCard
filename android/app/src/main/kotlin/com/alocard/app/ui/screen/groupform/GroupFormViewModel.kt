package com.alocard.app.ui.screen.groupform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alocard.app.domain.DateEngine
import com.alocard.app.domain.model.BillingGroup
import com.alocard.app.domain.repository.BillingGroupRepository
import com.alocard.app.domain.util.DateProvider
import com.alocard.app.domain.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** 账单组表单页状态（P4 / F2.1 / F2.4） */
data class GroupFormUiState(
    val isEditing: Boolean = false,
    val groupId: Long = -1L,
    val name: String = "",
    val nameError: String? = null,
    val billingDayText: String = "",
    val billingDayError: String? = null,
    val dueOffsetDays: Int = 20,
    val creditLimitText: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    /** 实时换算回显：账单日 X 号 → 本期还款日（PRD 5.2.3，锚点 = 本期账单日） */
    val duePreview: String?
        get() {
            val day = billingDayText.trim().toIntOrNull() ?: return null
            val today = DateProvider.today()
            val anchor = DateEngine.currentBillingDate(day, today)
            val due = anchor.plusDays(dueOffsetDays.toLong())
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
                    dueOffsetDays = group.dueOffsetDays ?: 20,
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
            day == null || day !in 1..28 -> "账单日须为 1–28 的整数"
            else -> null
        }
        _ui.update { it.copy(billingDayText = value, billingDayError = error) }
    }

    fun onDueOffsetChange(offset: Int) =
        _ui.update { it.copy(dueOffsetDays = offset.coerceIn(1, 28)) }

    /** 「固定几号」→ N（锚点为本期账单日，PRD 5.2.3） */
    fun onFixedDayCommit(fixedDayText: String) {
        val day = fixedDayText.trim().toIntOrNull() ?: return
        val billingDay = _ui.value.billingDayText.trim().toIntOrNull() ?: return
        val anchor = DateEngine.currentBillingDate(billingDay, DateProvider.today())
        var target = anchor.withDayOfMonth(day.coerceIn(1, 28))
        if (!target.isAfter(anchor)) target = target.plusMonths(1)
        val n = ChronoUnit.DAYS.between(anchor, target).toInt()
        _ui.update { it.copy(dueOffsetDays = n.coerceIn(1, 28)) }
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
            if (state.billingDayText.isNotBlank() && (day == null || day !in 1..28)) {
                _ui.update { it.copy(saving = false, billingDayError = "账单日须为 1–28 的整数") }
                return@launch
            }
            val group = BillingGroup(
                id = if (state.isEditing) state.groupId else 0L,
                name = state.name.trim(),
                billingDay = day,
                // 账单日填了而 N 缺省 → 20（PRD 4.2 默认值）；账单日空则整体待完善
                dueOffsetDays = if (day != null) state.dueOffsetDays else null,
                creditLimitCents = Money.parseToCents(state.creditLimitText),
                bankName = null, // 银行名由组内卡片识别填充，表单不直接维护
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
