package org.company.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.company.app.data.repository.RemoteSettings
import org.company.app.data.repository.RemoteSettingsRepository

class RemoteSettingsViewModel(
    private val repository: RemoteSettingsRepository
) : ViewModel() {
    private val _settings = MutableStateFlow(RemoteSettings())
    val settings: StateFlow<RemoteSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { value ->
                _settings.value = value
            }
        }
    }
}
