package com.alosir.card.ui.screen.groupdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.model.GroupWithCards
import com.alosir.card.domain.repository.BillingGroupRepository
import com.alosir.card.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 账单组详情页状态（P5 / F2.3） */
data class GroupDetailUiState(
    val loading: Boolean = true,
    val group: GroupWithCards? = null,
    /** 删除确认文案中列出的卡片数与账单期数 */
    val confirmDelete: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val billingGroups: BillingGroupRepository,
    private val cards: CardRepository,
) : ViewModel() {

    private val confirmDelete = MutableStateFlow(false)
    private val deleted = MutableStateFlow(false)
    private val flowCache = mutableMapOf<Long, StateFlow<GroupDetailUiState>>()

    /* 按 groupId 缓存 StateFlow：组合期重复调用不得新建流，
       否则 collectAsState 反复重置为初值，造成进入页面时闪烁。 */
    fun uiState(groupId: Long): StateFlow<GroupDetailUiState> =
        flowCache.getOrPut(groupId) {
            kotlinx.coroutines.flow.combine(
                billingGroups.observeGroup(groupId),
                confirmDelete,
                deleted,
            ) { group, confirming, gone ->
                GroupDetailUiState(
                    loading = group == null && !gone,
                    group = group,
                    confirmDelete = confirming,
                    deleted = gone,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailUiState())
        }

    fun showDeleteConfirm() = confirmDelete.update { true }
    fun dismissDeleteConfirm() = confirmDelete.update { false }

    /** 删除账单组：级联删除组内卡片与账单（PRD F2.5） */
    fun delete(groupId: Long) {
        viewModelScope.launch {
            billingGroups.deleteGroup(groupId)
            deleted.value = true
        }
    }

    /** 移出/删除卡片：卡片数据移除，账单组与账单历史保留（PRD F1.3） */
    fun removeCard(cardId: Long) {
        viewModelScope.launch { cards.deleteCard(cardId) }
    }
}
