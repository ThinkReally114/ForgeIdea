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

    fun getZenApiKey(): String? = prefs.getString(KEY_ZEN, null)

    fun setZenApiKey(key: String?) {
        if (key == null) prefs.edit().remove(KEY_ZEN).apply()
        else prefs.edit().putString(KEY_ZEN, key).apply()
    }

    fun getExaApiKey(): String? = prefs.getString(KEY_EXA, null)

    fun setExaApiKey(key: String?) {
        if (key == null) prefs.edit().remove(KEY_EXA).apply()
        else prefs.edit().putString(KEY_EXA, key).apply()
    }

    companion object {
        private const val KEY_ZEN = "zen_api_key"
        private const val KEY_EXA = "exa_api_key"
    }
}
