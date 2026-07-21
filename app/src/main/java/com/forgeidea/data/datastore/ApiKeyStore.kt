package com.forgeidea.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.PresetTheme
import com.forgeidea.domain.model.Provider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ApiKeyStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "api_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val plainPrefs: SharedPreferences = context.getSharedPreferences("forgeidea_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    fun setApiKey(key: String?) {
        if (key == null) prefs.edit().remove(KEY_API_KEY).apply()
        else prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)

    fun setBaseUrl(url: String?) {
        if (url == null) prefs.edit().remove(KEY_BASE_URL).apply()
        else prefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun getProviders(): List<Provider> {
        val json = prefs.getString(KEY_PROVIDERS, null)
        val providers = if (json == null) emptyList() else try {
            Json.decodeFromString<List<Provider>>(json)
        } catch (e: Exception) {
            emptyList()
        }
        return if (providers.isEmpty()) {
            migrateLegacyProvider()
        } else providers
    }

    fun setProviders(providers: List<Provider>) {
        prefs.edit().putString(KEY_PROVIDERS, Json.encodeToString(providers)).apply()
    }

    fun getProviderById(id: String): Provider? = getProviders().find { it.id == id }

    fun getProviderForModel(modelId: String): Provider? {
        val model = getModels().find { it.id == modelId } ?: return null
        return getProviderById(model.providerId)
            ?: getProviders().firstOrNull()
            ?: migrateLegacyProvider().firstOrNull()
    }

    fun getModels(): List<LlmModel> {
        val json = plainPrefs.getString(KEY_MODELS, null) ?: return defaultModels()
        return try {
            val models = Json.decodeFromString<List<LlmModel>>(json)
            migrateModelProviderIds(models)
        } catch (e: Exception) {
            defaultModels()
        }
    }

    fun setModels(models: List<LlmModel>) {
        plainPrefs.edit().putString(KEY_MODELS, Json.encodeToString(models)).apply()
    }

    fun getSelectedModelId(): String {
        return plainPrefs.getString(KEY_SELECTED_MODEL, null)
            ?: getModels().firstOrNull()?.id
            ?: "opencode/big-pickle"
    }

    fun setSelectedModelId(id: String) {
        plainPrefs.edit().putString(KEY_SELECTED_MODEL, id).apply()
    }

    fun getTheme(): PresetTheme {
        val name = plainPrefs.getString(KEY_THEME, null) ?: return PresetTheme.QZ_PURPLE
        return runCatching { PresetTheme.valueOf(name) }.getOrDefault(PresetTheme.QZ_PURPLE)
    }

    fun setTheme(theme: PresetTheme) {
        plainPrefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun getCurrentSessionId(): String? = plainPrefs.getString(KEY_CURRENT_SESSION, null)

    fun setCurrentSessionId(id: String?) {
        if (id == null) plainPrefs.edit().remove(KEY_CURRENT_SESSION).apply()
        else plainPrefs.edit().putString(KEY_CURRENT_SESSION, id).apply()
    }

    private fun migrateLegacyProvider(): List<Provider> {
        val baseUrl = getBaseUrl() ?: "https://api.opencode.ai/v1"
        val provider = Provider(
            id = "default",
            name = "默认服务商",
            baseUrl = baseUrl,
            apiKey = getApiKey() ?: ""
        )
        val list = listOf(provider)
        setProviders(list)
        return list
    }

    private fun migrateModelProviderIds(models: List<LlmModel>): List<LlmModel> {
        val providers = getProviders()
        val defaultProviderId = providers.firstOrNull()?.id ?: ""
        if (defaultProviderId.isBlank() || models.all { it.providerId.isNotBlank() }) {
            return models
        }
        val migrated = models.map {
            if (it.providerId.isBlank()) it.copy(providerId = defaultProviderId) else it
        }
        setModels(migrated)
        return migrated
    }

    private fun defaultModels(): List<LlmModel> = listOf(
        LlmModel(id = "opencode/big-pickle", name = "Big Pickle", providerId = "")
    )

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_PROVIDERS = "providers"
        private const val KEY_MODELS = "models"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEY_THEME = "theme"
        private const val KEY_CURRENT_SESSION = "current_session"
    }
}
