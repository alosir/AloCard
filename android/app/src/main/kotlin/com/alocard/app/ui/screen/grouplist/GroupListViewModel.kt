package com.alocard.app.ui.screen.grouplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alocard.app.domain.model.GroupQuota
import com.alocard.app.domain.repository.BillingGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 账单组管理页状态（P14） */
data class GroupListUiState(
    val loading: Boolean = true,
    val rows: List<GroupQuota> = emptyList(),
)

@HiltViewModel
class GroupListViewModel @Inject constructor(
    billingGroups: BillingGroupRepository,
) : ViewModel() {

    val uiState: StateFlow<GroupListUiState> =
        combine(
            billingGroups.observeGroupQuotas(),
            billingGroups.observeGroupsWithCards(),
        ) { quotas, groups ->
            val cardCountByGroup = groups.associate { it.id to it.cards.size }
            // 组维度信息合并（卡片数来自聚合视图）
            GroupListUiState(
                loading = false,
                rows = quotas.map { q ->
                    q.copy(name = q.name + if ((cardCountByGroup[q.groupId] ?: 0) > 1) {
                        " · 共享账单 ${cardCountByGroup[q.groupId]} 张卡"
                    } else "")
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupListUiState())
}
