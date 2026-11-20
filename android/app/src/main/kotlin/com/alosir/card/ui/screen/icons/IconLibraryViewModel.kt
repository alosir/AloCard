package com.alosir.card.ui.screen.icons

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.data.icon.CustomIcon
import com.alosir.card.data.icon.IconRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IconLibraryViewModel @Inject constructor(
    private val repository: IconRepository,
) : ViewModel() {
    val icons: StateFlow<List<CustomIcon>> = repository.observeIcons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun importIcon(uri: Uri, centerX: Float, centerY: Float, zoom: Float, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.importIcon(uri, centerX, centerY, zoom) }
            onDone()
        }
    }

    fun updateCrop(id: String, centerX: Float, centerY: Float, zoom: Float, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.updateCrop(id, centerX, centerY, zoom) }
            onDone()
        }
    }

    fun deleteIcon(id: String) {
        viewModelScope.launch { repository.deleteIcon(id) }
    }
}
