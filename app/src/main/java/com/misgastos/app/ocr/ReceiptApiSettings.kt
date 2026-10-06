package com.misgastos.app.ocr

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject

data class OcrSettings(
    val providerId: String = "local",
    val config: Map<String, String> = emptyMap(),
)

/**
 * Almacenamiento cifrado de la configuración de proveedores OCR.
 * Es genérico: guarda el id del proveedor y un mapa de valores indexado
 * por ProviderField.id (apiKey, model, baseUrl...), de modo que cualquier
 * proveedor nuevo de :core-ocr funciona sin cambios aquí.
 */
object ReceiptApiSettings {

    private const val PREFS_NAME = "receipt_api_settings"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_CONFIG = "config_json"
    // Claves del formato antiguo (provider + api_key sueltos)
    private const val LEGACY_KEY_API_KEY = "api_key"

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
        val provider = prefs.getString(KEY_PROVIDER, "local")?.takeIf { it.isNotBlank() } ?: "local"
        val legacyKey = prefs.getString(LEGACY_KEY_API_KEY, "").orEmpty()
        val configJson = prefs.getString(KEY_CONFIG, null)
        val config: Map<String, String> = if (!configJson.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(configJson)
                obj.keys().asSequence().associateWith { key -> obj.optString(key) }
            }.getOrDefault(emptyMap())
        } else if (legacyKey.isNotBlank()) {
            // Migración desde el formato antiguo: la API key suelta pasa a "apiKey"
            mapOf("apiKey" to legacyKey)
        } else {
            emptyMap()
        }
        return OcrSettings(providerId = provider, config = config)
    }

    fun save(context: Context, providerId: String, config: Map<String, String>) {
        val obj = JSONObject()
        config.forEach { (k, v) -> obj.put(k, v.trim()) }
        prefs(context).edit()
            .putString(KEY_PROVIDER, providerId)
            .putString(KEY_CONFIG, obj.toString())
            .apply()
    }
}
