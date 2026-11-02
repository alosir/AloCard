package com.alocard.app.ui.screen.addcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alocard.app.domain.CardNumberIdentifier
import com.alocard.app.domain.model.BillingGroup
import com.alocard.app.domain.repository.BillingGroupRepository
import com.alocard.app.domain.repository.BinDirectory
import com.alocard.app.domain.repository.CardRepository
import com.alocard.app.domain.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 归属方式 */
enum class GroupChoice { NEW, JOIN }

/** 还款日输入方式（ADR-0003） */
enum class DueInputMode { OFFSET_DAYS, FIXED_DAY }

/** 新增/编辑卡片页状态（P3 / F1.2） */
data class AddCardUiState(
    val isEditing: Boolean = false,
    val cardId: Long = -1L,
    // 卡片信息（全部非必填，ADR-0009）
    val cardNumber: String = "",
    val cardNumberError: String? = null,
    val cardName: String = "",
    val expiryYm: String = "",
    val expiryError: String? = null,
    val bankName: String = "",
    val cardScheme: String = "unknown",
    /** 银行/卡组织来自号段自动识别（用户覆盖后清除标注，PRD F1.1） */
    val detectedFromNumber: Boolean = false,
    val cardNumberNote: String? = null,
    // 账单组
    val choice: GroupChoice = GroupChoice.NEW,
    val matchedGroup: BillingGroup? = null,
    val groupName: String = "",
    val billingDayText: String = "",
    val billingDayError: String? = null,
    val dueOffsetDays: Int = 20,
    val dueInputMode: DueInputMode = DueInputMode.OFFSET_DAYS,
    val fixedDayText: String = "",
    val creditLimitText: String = "",
    // 结果
    val saving: Boolean = false,
    val savedGroupId: Long? = null,
    val error: String? = null,
) {
    val canSave: Boolean
        get() = !saving && cardNumberError == null && expiryError == null && billingDayError == null
}

@HiltViewModel
class AddCardViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val groupRepository: BillingGroupRepository,
    private val binDirectory: BinDirectory,
) : ViewModel() {

    private val _ui = MutableStateFlow(AddCardUiState())
    val ui: StateFlow<AddCardUiState> = _ui.asStateFlow()

    /** 编辑模式：预填已有卡片 */
    fun loadCard(cardId: Long) {
        if (_ui.value.isEditing) return
        viewModelScope.launch {
            val card = cardRepository.observeCard(cardId).first() ?: return@launch
            _ui.update {
                it.copy(
                    isEditing = true,
                    cardId = card.id,
                    cardNumber = "",
                    cardNumberNote = if (card.hasCardNumber) "已保存完整卡号（输入新卡号可替换）" else null,
                    cardName = card.cardLabel ?: "",
                    expiryYm = card.expiryYm ?: "",
                    bankName = card.bankName ?: "",
                    cardScheme = card.cardScheme,
                    choice = GroupChoice.NEW,
                )
            }
        }
    }

    /** 卡号失焦：校验 → Luhn 通过则触发号段识别（PRD F1.1） */
    fun onCardNumberChange(value: String) {
        _ui.update { it.copy(cardNumber = value, cardNumberError = null, cardNumberNote = null) }
    }

    fun onCardNumberDone() {
        val raw = _ui.value.cardNumber.trim()
        if (raw.isEmpty()) return
        val error = CardNumberIdentifier.validate(raw)
        if (error != null) {
            _ui.update { it.copy(cardNumberError = error) }
            return
        }
        val digits = CardNumberIdentifier.normalize(raw)
        if (!CardNumberIdentifier.luhnCheck(digits)) {
            _ui.update { it.copy(cardNumberError = "卡号校验未通过，请核对") }
            return
        }
        val match = binDirectory.lookup(digits)
        val scheme = CardNumberIdentifier.detectScheme(digits)
        _ui.update {
            it.copy(
                bankName = match?.bankName ?: it.bankName,
                cardScheme = CardNumberIdentifier.schemeKey(scheme),
                detectedFromNumber = match != null,
                cardNumberNote = when {
                    match != null -> "根据卡号识别${match.bankName}，识别结果仅供参考"
                    else -> "未识别到发卡行，请手动填写"
                },
            )
        }
        // 输入方式变化后重新做组匹配提示
        maybeMatchGroup()
    }

    fun onCardNameChange(value: String) = _ui.update { it.copy(cardName = value) }

    fun onExpiryChange(value: String) {
        val error = if (value.isEmpty()) null
        else if (!EXPIRY_RE.matches(value)) "有效期格式须为 YYYY-MM"
        else null
        _ui.update { it.copy(expiryYm = value, expiryError = error) }
    }

    fun onBankNameChange(value: String) =
        _ui.update { it.copy(bankName = value, detectedFromNumber = false) }

    fun onChoiceChange(choice: GroupChoice) = _ui.update { it.copy(choice = choice) }

    fun onGroupNameChange(value: String) = _ui.update { it.copy(groupName = value) }

    fun onBillingDayChange(value: String) {
        val day = value.trim().toIntOrNull()
        val error = when {
            value.trim().isEmpty() -> null
            day == null || day !in 1..28 -> "账单日须为 1–28 的整数"
            else -> null
        }
        _ui.update { it.copy(billingDayText = value, billingDayError = error) }
        maybeMatchGroup()
    }

    fun onDueOffsetChange(offset: Int) {
        _ui.update { it.copy(dueOffsetDays = offset.coerceIn(1, 28), dueInputMode = DueInputMode.OFFSET_DAYS) }
    }

    fun onFixedDayChange(value: String) {
        _ui.update { it.copy(fixedDayText = value, dueInputMode = DueInputMode.FIXED_DAY) }
    }

    /** 「固定几号」失焦：按锚点（本期账单日）换算为 N（PRD 5.2.3） */
    fun onFixedDayDone() {
        val day = _ui.value.fixedDayText.trim().toIntOrNull()
        val billingDay = _ui.value.billingDayText.trim().toIntOrNull()
        if (day == null || day !in 1..28 || billingDay == null) return
        val anchor = com.alocard.app.domain.DateEngine.currentBillingDate(
            billingDay, com.alocard.app.domain.util.DateProvider.today()
        )
        var target = anchor.withDayOfMonth(day)
        if (!target.isAfter(anchor)) target = target.plusMonths(1)
        val n = java.time.temporal.ChronoUnit.DAYS.between(anchor, target).toInt()
        _ui.update { it.copy(dueOffsetDays = n.coerceIn(1, 28)) }
    }

    fun onCreditLimitChange(value: String) = _ui.update { it.copy(creditLimitText = value) }

    /** 「同一银行 + 账单日相同」命中已有组时提示加入（PRD F2.2） */
    private fun maybeMatchGroup() {
        val bank = _ui.value.bankName.trim()
        val day = _ui.value.billingDayText.trim().toIntOrNull()
        if (bank.isEmpty() || day == null) {
            _ui.update { it.copy(matchedGroup = null) }
            return
        }
        viewModelScope.launch {
            val matched = groupRepository.matchGroup(bank, day)
            _ui.update {
                it.copy(
                    matchedGroup = matched,
                    choice = if (matched != null) GroupChoice.JOIN else it.choice,
                )
            }
        }
    }

    fun save() {
        val state = _ui.value
        if (state.saving) return

        // 编辑模式：只更新卡片基础信息
        if (state.isEditing) {
            viewModelScope.launch {
                _ui.update { it.copy(saving = true) }
                try {
                    val digits = state.cardNumber.takeIf { it.isNotBlank() }
                        ?.let { CardNumberIdentifier.normalize(it) }
                    cardRepository.updateCard(
                        cardId = state.cardId,
                        plainCardNumber = digits, // null = 不动已存卡号
                        cardNumberLast4 = digits?.takeLast(4),
                        expiryYm = state.expiryYm.takeIf { it.isNotBlank() },
                        cardScheme = state.cardScheme,
                        cardLabel = state.cardName.takeIf { it.isNotBlank() },
                        bankName = state.bankName.takeIf { it.isNotBlank() },
                    )
                    _ui.update { it.copy(savedGroupId = -2L) } // -2 = 编辑完成
                } catch (e: Exception) {
                    _ui.update { it.copy(saving = false, error = "保存失败：${e.message}") }
                }
            }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            try {
                val groupId = when (state.choice) {
                    GroupChoice.JOIN -> state.matchedGroup?.id
                        ?: return@launch _ui.update { it.copy(saving = false, error = "未选择要加入的账单组") }
                    GroupChoice.NEW -> {
                        val day = state.billingDayText.trim().toIntOrNull()
                        if (day != null && day !in 1..28) {
                            _ui.update { it.copy(saving = false, billingDayError = "账单日须为 1–28 的整数") }
                            return@launch
                        }
                        val name = state.groupName.trim().ifEmpty {
                            state.bankName.trim().ifEmpty { "未命名账单组" }
                        }
                        groupRepository.createGroup(
                            BillingGroup(
                                id = 0L,
                                name = name,
                                billingDay = day,
                                // 账单日填了而 N 缺省 → 20（PRD 4.2 默认值）
                                dueOffsetDays = if (day != null) state.dueOffsetDays else null,
                                creditLimitCents = Money.parseToCents(state.creditLimitText),
                                bankName = state.bankName.trim().ifEmpty { null },
                            )
                        )
                    }
                }
                val digits = state.cardNumber.takeIf { it.isNotBlank() }
                    ?.let { CardNumberIdentifier.normalize(it) }
                cardRepository.createCard(
                    groupId = groupId,
                    plainCardNumber = digits,
                    cardNumberLast4 = digits?.takeLast(4),
                    expiryYm = state.expiryYm.takeIf { it.isNotBlank() },
                    cardScheme = state.cardScheme,
                    cardLabel = state.cardName.takeIf { it.isNotBlank() },
                    bankName = state.bankName.takeIf { it.isNotBlank() },
                )
                _ui.update { it.copy(savedGroupId = groupId) }
            } catch (e: Exception) {
                _ui.update { it.copy(saving = false, error = "保存失败：${e.message}") }
            }
        }
    }

    companion object {
        val EXPIRY_RE = Regex("""^\d{4}-(0[1-9]|1[0-2])$""")
    }
}
