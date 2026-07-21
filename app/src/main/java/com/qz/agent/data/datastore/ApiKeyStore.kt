package com.qz.agent.data.datastore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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

    fun getModel(): String? = prefs.getString(KEY_MODEL, null)

    fun setModel(model: String?) {
        if (model == null) prefs.edit().remove(KEY_MODEL).apply()
        else prefs.edit().putString(KEY_MODEL, model).apply()
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"
    }
}
