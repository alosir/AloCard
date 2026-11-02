package com.alosir.card.ui.screen.quotadetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.model.CreditTotals
import com.alosir.card.domain.model.GroupQuota
import com.alosir.card.domain.repository.BillingGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class QuotaDetailViewModel @Inject constructor(
    billingGroups: BillingGroupRepository,
) : ViewModel() {

    val totals: StateFlow<CreditTotals> = billingGroups.observeTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreditTotals(0L, 0L))

    val rows: StateFlow<List<GroupQuota>> = billingGroups.observeGroupQuotas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
