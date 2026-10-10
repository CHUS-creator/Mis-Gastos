package com.misgastos.app.ocr

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class OcrSettings(
    val provider: ReceiptApiProvider = ReceiptApiProvider.LOCAL,
    val hasApiKey: Boolean = false,
)

object ReceiptApiSettings {

    private const val PREFS_NAME = "receipt_api_settings"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_API_KEY = "api_key"

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
        )
    }

    /** Limpia una clave pegada: quita espacios, saltos de línea y comillas envolventes. */
    fun sanitizeKey(raw: String): String {
        var key = raw.trim()
        // Quita comillas o backticks envolventes ("clave", 'clave', `clave`)
        while (key.length >= 2 &&
            ((key.first() == '"' && key.last() == '"') ||
                (key.first() == '\'' && key.last() == '\'') ||
                (key.first() == '`' && key.last() == '`'))
        ) {
            key = key.substring(1, key.length - 1).trim()
        }
        // Quita espacios y saltos de línea internos (pegados desde consolas/pdf)
        return key.filterNot { it.isWhitespace() }
    }

    fun save(context: Context, provider: ReceiptApiProvider, apiKey: String) {
        prefs(context).edit()
            .putString(KEY_PROVIDER, provider.name)
            .putString(KEY_API_KEY, sanitizeKey(apiKey))
            .apply()
    }

    fun apiKey(context: Context): String =
        sanitizeKey(prefs(context).getString(KEY_API_KEY, "").orEmpty())
}
