package com.forgeidea.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.PresetTheme
import com.forgeidea.domain.model.Provider
import com.forgeidea.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _providers = MutableStateFlow(apiKeyStore.getProviders())
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    val selectedTheme: StateFlow<PresetTheme> = themeViewModel.theme

    fun setApiKey(key: String) {
        viewModelScope.launch {
            apiKeyStore.setApiKey(if (key.isBlank()) null else key)
            _apiKey.value = key
            syncDefaultProvider()
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            apiKeyStore.setBaseUrl(if (url.isBlank()) null else url)
            _baseUrl.value = url
            syncDefaultProvider()
        }
    }

    private fun syncDefaultProvider() {
        val list = _providers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == "default" }
        val updated = if (idx >= 0) {
            list[idx] = list[idx].copy(
                baseUrl = _baseUrl.value,
                apiKey = _apiKey.value
            )
            list
        } else {
            list + Provider(
                id = "default",
                name = "默认服务商",
                baseUrl = _baseUrl.value,
                apiKey = _apiKey.value
            )
        }
        _providers.value = updated
        apiKeyStore.setProviders(updated)
        val models = _models.value.map {
            if (it.providerId.isBlank()) it.copy(providerId = "default") else it
        }
        if (models != _models.value) {
            _models.value = models
            apiKeyStore.setModels(models)
        }
    }

    fun addProvider(provider: Provider) {
        val updated = _providers.value + provider
        _providers.value = updated
        apiKeyStore.setProviders(updated)
    }

    fun updateProvider(oldId: String, provider: Provider) {
        val updated = _providers.value.map { if (it.id == oldId) provider else it }
        _providers.value = updated
        apiKeyStore.setProviders(updated)
        if (oldId != provider.id) {
            val models = _models.value.map {
                if (it.providerId == oldId) it.copy(providerId = provider.id) else it
            }
            _models.value = models
            apiKeyStore.setModels(models)
        }
    }

    fun removeProvider(id: String) {
        if (id == "default") return
        val updated = _providers.value.filter { it.id != id }
        _providers.value = updated
        apiKeyStore.setProviders(updated)
        val models = _models.value.map {
            if (it.providerId == id) it.copy(providerId = "default") else it
        }
        _models.value = models
        apiKeyStore.setModels(models)
    }

    fun addModel(model: LlmModel) {
        val providerId = model.providerId.takeIf { it.isNotBlank() }
            ?: _providers.value.firstOrNull()?.id
            ?: ""
        val normalized = model.copy(providerId = providerId)
        val updated = _models.value + normalized
        _models.value = updated
        apiKeyStore.setModels(updated)
    }

    fun updateModel(oldId: String, model: LlmModel) {
        val providerId = model.providerId.takeIf { it.isNotBlank() }
            ?: _providers.value.firstOrNull()?.id
            ?: ""
        val normalized = model.copy(providerId = providerId)
        val updated = _models.value.map { if (it.id == oldId) normalized else it }
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
