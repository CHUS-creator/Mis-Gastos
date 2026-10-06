package com.misgastos.ocr.api

/**
 * Contrato que debe implementar cualquier proveedor de extracción de tickets.
 *
 * La app solo conoce esta interfaz; añadir una API nueva = crear una clase
 * que la implemente y registrarla en [OcrProviderRegistry].
 */
interface OcrProvider {
    /** Identificador estable, p. ej. "mistral". Se persiste en ajustes. */
    val id: String

    /** Nombre visible en la UI de ajustes. */
    val displayName: String

    /** Campos que este proveedor necesita del usuario (API key, modelo, URL...). */
    val configSchema: List<ProviderField>

    /**
     * Extrae los datos del ticket.
     *
     * @param config valores ya descifrados, indexados por [ProviderField.id].
     *               El almacenamiento cifrado (EncryptedSharedPreferences, etc.)
     *               es responsabilidad de la app anfitriona, no de la biblioteca.
     * @throws OcrException ante errores de red, HTTP o formato de respuesta.
     */
    suspend fun extract(request: OcrRequest, config: Map<String, String>): OcrReceipt
}

/** Utilidad para proveedores: valor requerido o excepción clara. */
fun Map<String, String>.required(fieldId: String, providerId: String): String =
    this[fieldId]?.takeIf { it.isNotBlank() }
        ?: throw OcrException("El proveedor '$providerId' requiere el campo '$fieldId'")
