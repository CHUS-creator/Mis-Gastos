package com.misgastos.app.ocr

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class OcrSettings(
    val provider: ReceiptApiProvider = ReceiptApiProvider.LOCAL,
    val hasApiKey: Boolean = false,
    val model: String = "",
)

object ReceiptApiSettings {

    private const val PREFS_NAME = "receipt_api_settings"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model"

    fun prefs(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    fun load(context: Context): OcrSettings {
        val prefs = prefs(context)
        val provider = prefs.getString(KEY_PROVIDER, ReceiptApiProvider.LOCAL.name)
            ?.let { runCatching { ReceiptApiProvider.valueOf(it) }.getOrNull() }
            ?: ReceiptApiProvider.LOCAL
        return OcrSettings(
            provider = provider,
            hasApiKey = prefs.getString(KEY_API_KEY, "").orEmpty().isNotBlank(),
            model = prefs.getString(KEY_MODEL, "").orEmpty(),
        )
    }

    fun save(context: Context, provider: ReceiptApiProvider, apiKey: String, model: String = "") {
        prefs(context).edit()
            .putString(KEY_PROVIDER, provider.name)
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_MODEL, model.trim())
            .apply()
    }

    fun apiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "").orEmpty()
}
