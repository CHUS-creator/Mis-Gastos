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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.misgastos.app.R
import com.misgastos.app.ocr.ReceiptApiProvider
import com.misgastos.app.ocr.ReceiptApiSettings

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
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; savedFeedback = false },
                    label = { Text(stringResource(R.string.settings_ocr_api_key)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
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
        }
    }
}
