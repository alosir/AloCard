package com.alosir.card.ui.screen.grouplist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.DateEngine
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.domain.repository.BillRepository
import com.alosir.card.domain.util.DateProvider
import com.alosir.card.domain.util.Money
import com.alosir.card.ui.common.AmountKeypad
import com.alosir.card.ui.common.FilledButton
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.TonalButton
import com.alosir.card.ui.common.amountInputFilter
import com.alosir.card.ui.components.BankLogo
import com.alosir.card.ui.theme.AloCardTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** 「选择信用卡」候选：最近一期账单未录入的组（文案上 = 信用卡，不叫账单组） */
data class AddBillCandidate(
    val groupId: Long,
    val bankName: String,
    val tails: List<String>,
    val billDate: LocalDate,
    val daysSinceBill: Int,
)

@HiltViewModel
class AddBillViewModel @Inject constructor(
    private val billingGroups: BillingGroupRepository,
    private val bills: BillRepository,
) : ViewModel() {

    val candidates: StateFlow<List<AddBillCandidate>> =
        billingGroups.observeGroupsWithCards().map { groups ->
            val today = DateProvider.today()
            groups
                .filter { it.currentBill?.billAmountCents == null && it.billingDay != null }
                .map { g ->
                    val date = g.currentBill?.billingDate
                        ?: DateEngine.currentBillingDate(g.billingDay!!, today)
                    AddBillCandidate(
                        groupId = g.id,
                        bankName = g.bankName ?: g.name,
                        tails = g.cards.mapNotNull { it.cardNumberLast4 },
                        billDate = date,
                        daysSinceBill = ChronoUnit.DAYS.between(date, today).toInt(),
                    )
                }
                // 最近一期账单日距今越小的越靠前
                .sortedBy { it.daysSinceBill }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedGroupId = MutableStateFlow<Long?>(null)
    val amountText = MutableStateFlow("")
    val amountError = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    val saved = MutableStateFlow(false)

    fun select(id: Long) { selectedGroupId.value = id }

    fun onAmountChange(value: String) {
        amountText.value = value
        amountError.value = null
    }

    fun save() {
        val groupId = selectedGroupId.value ?: candidates.value.firstOrNull()?.groupId ?: return
        val cents = Money.parseToCents(amountText.value)
        if (cents == null) {
            amountError.value = "请输入有效金额"
            return
        }
        val date = candidates.value.firstOrNull { it.groupId == groupId }?.billDate ?: return
        viewModelScope.launch {
            saving.value = true
            bills.saveBillAmount(groupId, date, cents)
            saving.value = false
            saved.value = true
        }
    }
}

/**
 * 添加账单弹窗 · v0.0.5。
 * 选择信用卡（待录入组，账单日近者在前）+ 金额输入（APP 键盘，可长按粘贴）+ 保存。
 * 列表最多 3 条，超出内部滚动；只有 1 条/多条时默认选中第一条。
 */
@Composable
fun AddBillSheet(
    onDismiss: () -> Unit,
    onAddCard: () -> Unit,
    viewModel: AddBillViewModel = hiltViewModel(),
) {
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val amountText by viewModel.amountText.collectAsStateWithLifecycle()
    val amountError by viewModel.amountError.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val colors = AloCardTheme.colors
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(candidates) {
        if (candidates.isNotEmpty() && selectedId == null) {
            viewModel.select(candidates.first().groupId)
        }
    }
    LaunchedEffect(saved) { if (saved) onDismiss() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable { onDismiss() },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.92f).dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.paperRaised)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("添加账单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp).clickable { onDismiss() }.padding(4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))

            // ---- 选择信用卡 ----
            Text("选择信用卡", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            if (candidates.isEmpty()) {
                HintText("最近一期账单都已录入。新增卡片可在这里补录账单。")
                Spacer(Modifier.height(8.dp))
                FilledButton("添加新卡片", onAddCard, Modifier.fillMaxWidth())
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 186.dp) // 候选最多 3 条，超出内部滚动
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.paperSunken),
                ) {
                    items(candidates, key = { it.groupId }) { c ->
                        val selected = selectedId == c.groupId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (selected) colors.goldSoft else Color.Transparent)
                                .clickable { viewModel.select(c.groupId) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BankLogo(bankName = c.bankName, glyph = c.bankName.first().toString(), color = Color.Transparent, size = 26.dp)
                            Spacer(Modifier.size(10.dp))
                            Text(
                                "${c.bankName}（${c.tails.joinToString("/")}）",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) colors.goldText else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // ---- 金额输入（APP 键盘 + 可长按粘贴） ----
                Text("本期账单金额（元）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = viewModel::onAmountChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    // 不用系统键盘（APP 内键盘输入）；长按仍可用系统「粘贴」填入金额
                    modifier = Modifier.onFocusChanged { state ->
                        if (state.isFocused) keyboardController?.hide()
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.paperSunken)
                                .border(
                                    1.dp,
                                    if (amountError != null) MaterialTheme.colorScheme.error else Color.Transparent,
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (amountText.isEmpty()) {
                                Text("0.00", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    },
                )
                if (amountError != null) {
                    HintText(amountError!!, Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.error)
                } else {
                    HintText("最多 2 位小数；可长按输入框粘贴金额", Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TonalButton("取消", onDismiss, Modifier.weight(1f))
                    FilledButton(
                        text = if (saving) "保存中…" else "保存",
                        onClick = viewModel::save,
                        modifier = Modifier.weight(2f),
                        enabled = !saving && amountText.isNotBlank(),
                    )
                }
            }
            if (candidates.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                AmountKeypad(onKey = { key -> viewModel.onAmountChange(amountInputFilter(amountText, key)) })
            }
        }
    }
}
}
