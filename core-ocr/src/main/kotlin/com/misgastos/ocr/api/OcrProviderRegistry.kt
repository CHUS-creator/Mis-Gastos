package com.misgastos.ocr.api

/**
 * Registro central de proveedores. La app lo inicializa una vez
 * (por ejemplo en la clase Application) y consulta desde ahí.
 */
class OcrProviderRegistry {

    private val providers = linkedMapOf<String, OcrProvider>()

    fun register(provider: OcrProvider) {
        providers[provider.id] = provider
    }

    fun registerAll(providers: List<OcrProvider>) = providers.forEach(::register)

    fun get(id: String): OcrProvider? = providers[id]

    fun all(): List<OcrProvider> = providers.values.toList()

    fun ids(): List<String> = providers.keys.toList()

    companion object {
        /** Registro por defecto con los proveedores incluidos en la biblioteca. */
        fun withBundledProviders(): OcrProviderRegistry =
            OcrProviderRegistry().apply { registerAll(BundledProviders.all()) }
    }
}

/** Proveedores que vienen con la biblioteca. */
object BundledProviders {
    fun all(): List<OcrProvider> = listOf(
        com.misgastos.ocr.providers.LocalReceiptProvider(),
        com.misgastos.ocr.providers.MistralOcrProvider(),
        com.misgastos.ocr.providers.GeminiOcrProvider(),
        com.misgastos.ocr.providers.CustomHttpProvider(),
    )
}
