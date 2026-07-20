package com.qz.agent.ui.settings

import androidx.lifecycle.ViewModel
import com.qz.agent.data.datastore.ApiKeyStore
import com.qz.agent.domain.model.PresetTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class SettingsViewModel(
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    private val _zenApiKey = MutableStateFlow(apiKeyStore.getZenApiKey() ?: "")
    val zenApiKey: StateFlow<String> = _zenApiKey.asStateFlow()

    private val _selectedTheme = MutableStateFlow(PresetTheme.QZ_PURPLE)
    val selectedTheme: StateFlow<PresetTheme> = _selectedTheme.asStateFlow()

    fun setZenApiKey(key: String) {
        viewModelScope.launch {
            apiKeyStore.setZenApiKey(if (key.isBlank()) null else key)
            _zenApiKey.value = key
        }
    }

    fun setTheme(theme: PresetTheme) {
        _selectedTheme.value = theme
    }
}
