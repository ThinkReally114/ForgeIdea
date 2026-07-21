package com.forgeidea.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.forgeidea.domain.model.LlmModel
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

    fun getModels(): List<LlmModel> {
        val json = plainPrefs.getString(KEY_MODELS, null) ?: return defaultModels()
        return try {
            Json.decodeFromString(json)
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

    private fun defaultModels(): List<LlmModel> = listOf(
        LlmModel(id = "opencode/big-pickle", name = "Big Pickle")
    )

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODELS = "models"
        private const val KEY_SELECTED_MODEL = "selected_model"
    }
}
