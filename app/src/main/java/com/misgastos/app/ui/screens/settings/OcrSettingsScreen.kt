package com.misgastos.app.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.misgastos.app.R
import com.misgastos.app.ocr.OcrService
import com.misgastos.app.ocr.OcrSettings
import com.misgastos.app.ocr.ReceiptApiSettings
import kotlinx.coroutines.launch

private const val SAMPLE_RECEIPT = "MERCADONA\nAV. EJEMPLO 1\n22/09/2026\nAGUA MINERAL 6X0,29\nTOTAL 1,74"

/**
 * Pantalla de ajustes OCR genérica: los proveedores y sus campos de
 * configuración se obtienen del configSchema de :core-ocr, de modo que
 * añadir un proveedor nuevo no requiere tocar esta pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val saved = remember { ReceiptApiSettings.load(context) }
    val providers = remember { OcrService.providers() }
    var providerId by rememberSaveable { mutableStateOf(saved.providerId) }
    // Valores por campo, indexados por ProviderField.id
    val initialValues = providers.associate { p ->
        p.id to p.configSchema.associate { field ->
            field.id to (saved.config[field.id] ?: field.defaultValue)
        }
    }
    var fieldValues by remember { mutableStateOf(initialValues) }
    var savedFeedback by rememberSaveable { mutableStateOf(false) }
    var testRunning by rememberSaveable { mutableStateOf(false) }
    var testOk by rememberSaveable { mutableStateOf(false) }
    var testResult by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    val provider = providers.firstOrNull { it.id == providerId } ?: providers.first()

    fun currentConfig(): Map<String, String> =
        fieldValues[provider.id].orEmpty().filterValues { it.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_ocr_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_ocr_provider_hint),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                providers.forEach { p ->
                    FilterChip(
                        selected = p.id == providerId,
                        onClick = {
                            providerId = p.id
                            savedFeedback = false
                            testResult = null
                        },
                        label = { Text(p.displayName) },
                    )
                }
            }
            // Campos de configuración del proveedor seleccionado
            provider.configSchema.forEach { field ->
                val value = fieldValues[provider.id]?.get(field.id).orEmpty()
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        fieldValues = fieldValues.toMutableMap().apply {
                            put(
                                provider.id,
                                (get(provider.id) ?: emptyMap()).toMutableMap().apply {
                                    put(field.id, newValue)
                                },
                            )
                        }
                        savedFeedback = false
                    },
                    label = {
                        Text(field.label + if (!field.required) " (opcional)" else "")
                    },
                    singleLine = true,
                    visualTransformation = if (field.secret) PasswordVisualTransformation() else {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    },
                    placeholder = if (field.placeholder.isNotBlank()) {
                        { Text(field.placeholder) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = {
                    ReceiptApiSettings.save(context, provider.id, currentConfig())
                    savedFeedback = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_ocr_save))
            }
            if (savedFeedback) {
                Text(
                    text = stringResource(R.string.settings_ocr_saved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (provider.id != "local") {
                OutlinedButton(
                    onClick = {
                        testRunning = true
                        testResult = null
                        scope.launch {
                            val outcome = runCatching {
                                OcrService.extract(
                                    settings = OcrSettings(providerId = provider.id, config = currentConfig()),
                                    rawText = SAMPLE_RECEIPT,
                                )
                            }
                            testResult = outcome.fold(
                                onSuccess = { parsed ->
                                    if (parsed.source == com.misgastos.app.ocr.ReceiptSource.API) "" else {
                                        parsed.apiError ?: context.getString(R.string.ocr_error_unknown)
                                    }
                                },
                                onFailure = { it.message ?: context.getString(R.string.ocr_error_unknown) },
                            )
                            testOk = outcome.isSuccess && testResult.isNullOrEmpty()
                            testRunning = false
                        }
                    },
                    enabled = !testRunning && provider.configSchema
                        .filter { it.required }.all { currentConfig()[it.id]?.isNotBlank() == true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (testRunning) stringResource(R.string.settings_ocr_test_running)
                        else stringResource(R.string.settings_ocr_test)
                    )
                }
                if (testResult != null) {
                    Text(
                        text = stringResource(
                            if (testOk) R.string.settings_ocr_test_ok
                            else R.string.settings_ocr_test_failed,
                            testResult ?: "",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testOk) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        }
    }
}
