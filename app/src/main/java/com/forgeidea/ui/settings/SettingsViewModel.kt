package com.forgeidea.ui.settings

import androidx.lifecycle.ViewModel
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.PresetTheme
import com.forgeidea.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class SettingsViewModel(
    private val apiKeyStore: ApiKeyStore,
    private val themeViewModel: ThemeViewModel
) : ViewModel() {

    private val _apiKey = MutableStateFlow(apiKeyStore.getApiKey() ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _baseUrl = MutableStateFlow(
        apiKeyStore.getBaseUrl() ?: "https://api.opencode.ai/v1"
    )
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _models = MutableStateFlow(apiKeyStore.getModels())
    val models: StateFlow<List<LlmModel>> = _models.asStateFlow()

    val selectedTheme: StateFlow<PresetTheme> = themeViewModel.theme

    fun setApiKey(key: String) {
        viewModelScope.launch {
            apiKeyStore.setApiKey(if (key.isBlank()) null else key)
            _apiKey.value = key
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            apiKeyStore.setBaseUrl(if (url.isBlank()) null else url)
            _baseUrl.value = url
        }
    }

    fun addModel(model: LlmModel) {
        val updated = _models.value + model
        _models.value = updated
        apiKeyStore.setModels(updated)
    }

    fun updateModel(oldId: String, model: LlmModel) {
        val updated = _models.value.map { if (it.id == oldId) model else it }
        _models.value = updated
        apiKeyStore.setModels(updated)
    }

    fun removeModel(id: String) {
        val updated = _models.value.filter { it.id != id }
        _models.value = updated
        apiKeyStore.setModels(updated)
    }

    fun setTheme(theme: PresetTheme) {
        themeViewModel.setTheme(theme)
    }
}
