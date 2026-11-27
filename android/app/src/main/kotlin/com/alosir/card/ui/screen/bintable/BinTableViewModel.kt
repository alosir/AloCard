package com.alosir.card.ui.screen.bintable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.domain.repository.BinDirectory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BinTableViewModel @Inject constructor(
    binDirectory: BinDirectory,
) : ViewModel() {
    private val entries = binDirectory.entries()
    val snapshot: String? = binDirectory.snapshot()
    val query = MutableStateFlow("")

    val rows: StateFlow<List<BinDirectory.BinEntry>> = query
        .map { text ->
            val keyword = text.trim()
            if (keyword.isEmpty()) {
                entries
            } else {
                entries.filter { entry ->
                    entry.bankName.contains(keyword, ignoreCase = true) ||
                        entry.prefix.contains(keyword, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), entries)
}
