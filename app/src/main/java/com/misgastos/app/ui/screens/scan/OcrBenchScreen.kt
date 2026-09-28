package com.misgastos.app.ui.screens.scan

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.misgastos.app.R
import com.misgastos.app.viewmodel.MisGastosViewModel
import com.misgastos.app.viewmodel.OcrBenchResult
import com.misgastos.app.viewmodel.OcrBenchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrBenchScreen(
    viewModel: MisGastosViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.ocrBenchState.collectAsState()
    BackHandler(onBack = onBack)

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris -> if (uris.isNotEmpty()) viewModel.runOcrBench(uris) }

    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ocr_bench_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    if (state.results.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val text = viewModel.buildOcrBenchShareText(state.results)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                shareLauncher.launch(Intent.createChooser(intent, context.getString(R.string.ocr_bench_share)))
                            },
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.ocr_bench_share))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    pickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Text(stringResource(R.string.ocr_bench_pick))
            }
            if (state.running) {
                LinearProgressIndicator(
                    progress = { if (state.total > 0) state.processed.toFloat() / state.total else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.ocr_bench_progress, state.processed, state.total),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.results, key = { it.uri.toString() }) { result ->
                    OcrBenchResultCard(result)
                }
            }
        }
    }
}

@Composable
private fun OcrBenchResultCard(result: OcrBenchResult) {
    var expanded by androidx.compose.runtime.remember(result.uri) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = result.parsed?.merchant.orEmpty()
                        .ifBlank { stringResource(R.string.ocr_bench_no_merchant) },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = result.parsed?.total?.let { "%.2f".format(it) }
                        ?: stringResource(R.string.ocr_bench_no_total),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(
                    if (result.error != null) R.string.ocr_bench_status_error else R.string.ocr_bench_status_ok,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (result.error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.ocr_bench_hide else R.string.ocr_bench_show))
            }
            if (expanded) {
                Text(
                    text = result.rawText.ifBlank { result.error.orEmpty() },
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
