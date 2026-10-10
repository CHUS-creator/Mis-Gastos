package com.misgastos.ocr

import com.misgastos.ocr.api.OcrProviderRegistry
import com.misgastos.ocr.providers.LocalReceiptProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrProviderRegistryTest {

    @Test
    fun `registro por defecto contiene los proveedores incluidos`() {
        val registry = OcrProviderRegistry.withBundledProviders()
        assertEquals(listOf("local", "mistral", "gemini", "custom"), registry.ids())
    }

    @Test
    fun `se puede registrar un proveedor nuevo sin tocar los existentes`() {
        val registry = OcrProviderRegistry()
        registry.registerAll(
            listOf(
                LocalReceiptProvider(),
                FakeProvider("openai"),
            ),
        )
        assertTrue(registry.get("openai") is FakeProvider)
    }

    @Test
    fun `un id duplicado sobrescribe al proveedor anterior`() {
        val registry = OcrProviderRegistry()
        registry.register(FakeProvider("x", label = "Primero"))
        registry.register(FakeProvider("x", label = "Segundo"))
        assertEquals("Segundo", registry.get("x")?.displayName)
    }

    private class FakeProvider(override val id: String, private val label: String = id) :
        com.misgastos.ocr.api.OcrProvider {
        override val displayName = label
        override val configSchema = emptyList<com.misgastos.ocr.api.ProviderField>()
        override suspend fun extract(
            request: com.misgastos.ocr.api.OcrRequest,
            config: Map<String, String>,
        ) = com.misgastos.ocr.api.OcrReceipt()
    }
}
