package com.lengcs.fkwakeup.feature.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * 导入流程入口：输入 → 预览纠偏 / 失败闭环 → 落库。
 */
@Composable
fun ImportFlow(
    onImported: (Long) -> Unit,
    initialText: String? = null,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 从其他 App 分享进来的文本，预填到输入框
    LaunchedEffect(initialText) {
        if (!initialText.isNullOrBlank() && viewModel.inputText.isBlank()) {
            viewModel.onTextChanged(initialText)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(viewModel.stage, viewModel.importedTermId) {
        if (viewModel.stage == ImportStage.Done) {
            viewModel.importedTermId?.let { onImported(it) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when (viewModel.stage) {
            ImportStage.Input -> ImportScreen(viewModel = viewModel, modifier = modifier.padding(padding))
            ImportStage.Preview -> ImportPreviewScreen(viewModel = viewModel, modifier = modifier.padding(padding))
            ImportStage.Failure -> ImportFailureScreen(viewModel = viewModel, modifier = modifier.padding(padding))
            ImportStage.Done -> Unit // 由 LaunchedEffect 回调外部
        }
    }
}

@Composable
fun ImportFailureScreen(
    viewModel: ImportViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.import_fail_title),
            style = MaterialTheme.typography.titleLarge,
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                viewModel.errors.forEach { error ->
                    val prefix = error.recordIndex?.let { "第 $it 条记录：" }.orEmpty()
                    Text("$prefix${error.message}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Button(onClick = viewModel::copyErrorReport, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_fail_copy_report))
        }
        OutlinedButton(onClick = viewModel::backToInput, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_fail_manual))
        }
        OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_fail_retry))
        }
    }
}
