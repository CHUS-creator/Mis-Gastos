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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.misgastos.app.R
import com.misgastos.app.ocr.ReceiptApiClient
import com.misgastos.app.ocr.ReceiptApiConfig
import com.misgastos.app.ocr.ReceiptApiProvider
import com.misgastos.app.ocr.ReceiptApiSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val saved = remember { ReceiptApiSettings.load(context) }
    var provider by remember { mutableStateOf(saved.provider) }
    var apiKey by remember { mutableStateOf(ReceiptApiSettings.apiKey(context)) }
    var savedFeedback by remember { mutableStateOf(false) }
    var testRunning by remember { mutableStateOf(false) }
    var testOk by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

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
                FilterChip(
                    selected = provider == ReceiptApiProvider.LOCAL,
                    onClick = { provider = ReceiptApiProvider.LOCAL },
                    label = { Text(stringResource(R.string.settings_ocr_provider_local)) },
                )
                FilterChip(
                    selected = provider == ReceiptApiProvider.GEMINI,
                    onClick = { provider = ReceiptApiProvider.GEMINI },
                    label = { Text("Gemini") },
                )
                FilterChip(
                    selected = provider == ReceiptApiProvider.MISTRAL,
                    onClick = { provider = ReceiptApiProvider.MISTRAL },
                    label = { Text("Mistral") },
                )
            }
            if (provider != ReceiptApiProvider.LOCAL) {
                var showKey by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; savedFeedback = false },
                    label = { Text(stringResource(R.string.settings_ocr_api_key)) },
                    singleLine = true,
                    visualTransformation = if (showKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showKey) {
                                    stringResource(R.string.settings_ocr_hide_key)
                                } else {
                                    stringResource(R.string.settings_ocr_show_key)
                                },
                            )
                        }
                    },
                    isError = apiKey.isNotBlank() && ReceiptApiSettings.sanitizeKey(apiKey) != apiKey.trim(),
                    modifier = Modifier.fillMaxWidth(),
                )
                val cleanedKey = ReceiptApiSettings.sanitizeKey(apiKey)
                if (apiKey.isNotBlank()) {
                    Text(
                        text = when {
                            ReceiptApiSettings.sanitizeKey(apiKey) != apiKey.trim() ->
                                stringResource(R.string.settings_ocr_key_will_be_cleaned, cleanedKey.length)
                            else -> stringResource(R.string.settings_ocr_key_length, cleanedKey.length)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ReceiptApiSettings.sanitizeKey(apiKey) != apiKey.trim()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Text(
                    text = stringResource(
                        if (provider == ReceiptApiProvider.GEMINI) {
                            R.string.settings_ocr_hint_gemini
                        } else {
                            R.string.settings_ocr_hint_mistral
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = {
                    ReceiptApiSettings.save(context, provider, apiKey)
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
            if (provider != ReceiptApiProvider.LOCAL) {
                OutlinedButton(
                    onClick = {
                        testRunning = true
                        testResult = null
                        scope.launch {
                            val config = ReceiptApiConfig(provider, ReceiptApiSettings.sanitizeKey(apiKey))
                            val outcome = runCatching {
                                ReceiptApiClient.extract(
                                    config,
                                    "MERCADONA\nAV. EJEMPLO 1\n22/09/2026\nAGUA MINERAL 6X0,29\nTOTAL 1,74",
                                )
                            }
                            testResult = outcome.fold(
                                onSuccess = { result ->
                                    if (result.total != null) {
                                        "OK: total=${result.total}" +
                                            (result.merchant?.let { ", comercio=$it" } ?: "")
                                    } else {
                                        "La API respondió pero no devolvió datos útiles (revisa el resultado al escanear)"
                                    }
                                },
                                onFailure = {
                                    it.message?.ifBlank { it.javaClass.simpleName }
                                        ?: it.javaClass.simpleName
                                },
                            )
                            testOk = outcome.isSuccess
                            testRunning = false
                        }
                    },
                    enabled = !testRunning && apiKey.isNotBlank(),
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
