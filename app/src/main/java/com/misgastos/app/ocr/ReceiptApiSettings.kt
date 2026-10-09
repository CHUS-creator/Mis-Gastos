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
 *
 * Importante: la creación del almacenamiento cifrado es tolerante a fallos.
 * EncryptedSharedPreferences lanza excepciones sin capturar
 * (GeneralSecurityException / InvalidKeyException / AEADBadTagException...)
 * cuando el fichero cifrado o la master key del Keystore quedan invalidados,
 * p. ej. al actualizar la app sobre datos de una versión anterior. Eso
 * provocaba un FATAL EXCEPTION tanto al abrir "Ajustes de reconocimiento"
 * (ReceiptApiSettings.load se ejecuta durante la composición de la pantalla)
 * como al escanear un ticket (extractReceipt en MisGastosViewModel), porque
 * ambos caminos leen los ajustes. En una instalación limpia nunca falla,
 * por eso el repro en emulador no lo capturó.
 */
object ReceiptApiSettings {

    private const val PREFS_NAME = "receipt_api_settings"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_CONFIG = "config_json"
    // Claves del formato antiguo (provider + api_key sueltos)
    private const val LEGACY_KEY_API_KEY = "api_key"
    // Último recurso si el Keystore no permite cifrar en este dispositivo
    private const val FALLBACK_PREFS_NAME = "receipt_api_settings_plain"

    /** Ids de proveedor guardados por la versión anterior (enum en mayúsculas). */
    private val LEGACY_PROVIDER_IDS = mapOf(
        "LOCAL" to "local",
        "MISTRAL" to "mistral",
        "GEMINI" to "gemini",
    )

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    /**
     * Devuelve el almacenamiento cifrado, recreándolo si el fichero existente
     * está corrupto o ya no se puede descifrar, y cayendo a un fichero sin
     * cifrar solo si el Keystore no funciona en el dispositivo. Nunca lanza.
     */
    fun prefs(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        return synchronized(this) {
            cachedPrefs ?: createResilient(context).also { cachedPrefs = it }
        }
    }

    private fun createResilient(context: Context): SharedPreferences {
        // 1. Intento normal con el fichero cifrado existente.
        runCatching { createEncrypted(context, PREFS_NAME) }
            .getOrNull()?.let { return it }

        // 2. El fichero cifrado no se puede abrir (clave invalidada o datos
        //    corruptos): se descarta y se recrea limpio. Se pierde la
        //    configuración guardada, pero la app sigue funcionando y el
        //    usuario puede reconfigurar su proveedor en Ajustes.
        deletePrefsFile(context, PREFS_NAME)
        runCatching { createEncrypted(context, PREFS_NAME) }
            .getOrNull()?.let { return it }

        // 3. Keystore no utilizable en este dispositivo: mejor sin cifrar
        //    que un crash. Solo ocurre en casos patológicos.
        return context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun createEncrypted(context: Context, name: String): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            name,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private fun deletePrefsFile(context: Context, name: String) {
        runCatching {
            // Vaciar el XML y eliminar el fichero por si el proceso sigue
            // manteniéndolo mmap-eado.
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit().clear().commit()
            context.applicationInfo.dataDir?.let { base ->
                java.io.File(java.io.File(base, "shared_prefs"), "$name.xml").delete()
            }
        }
    }

    fun load(context: Context): OcrSettings = runCatching { loadInternal(context) }
        .getOrElse { OcrSettings() }

    private fun loadInternal(context: Context): OcrSettings {
        val prefs = prefs(context)
        val stored = prefs.getString(KEY_PROVIDER, "local").orEmpty()
        val provider = stored.takeIf { it.isNotBlank() }
            ?.let { LEGACY_PROVIDER_IDS[it.uppercase()] ?: it.lowercase() }
            ?: "local"
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
        runCatching {
            val obj = JSONObject()
            config.forEach { (k, v) -> obj.put(k, v.trim()) }
            prefs(context).edit()
                .putString(KEY_PROVIDER, providerId)
                .putString(KEY_CONFIG, obj.toString())
                .apply()
        }
    }
}
