package com.lengcs.fkwakeup.feature.importexport

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    modifier: Modifier = Modifier,
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.import_tab_ai), stringResource(R.string.import_tab_file))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabRow(
            selectedTabIndex = tabIndex,
            modifier = Modifier.clip(MaterialTheme.shapes.medium),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title) })
            }
        }

        if (tabIndex == 0) {
            AiTab(viewModel = viewModel)
        } else {
            FileTab(viewModel = viewModel)
        }
    }
}

@Composable
private fun AiTab(viewModel: ImportViewModel) {
    val context = LocalContext.current
    var promptExpanded by remember { mutableStateOf(false) }

    GuideCard()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = viewModel::copyPrompt, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.import_copy_prompt))
            }
            Text(
                text = stringResource(R.string.import_ai_support),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { promptExpanded = !promptExpanded }) {
                Text(stringResource(R.string.import_view_prompt))
            }
            if (promptExpanded) {
                Text(
                    text = remember(context) { ImportTexts.prompt(context) },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    PasteArea(viewModel = viewModel)
}

@Composable
private fun FileTab(viewModel: ImportViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val text = readTextFromUri(context, uri)
            if (text != null) viewModel.onTextChanged(text)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "选择此前由 AI 生成的课表 JSON 文件（如从电脑上导出的）",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = { launcher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.import_pick_file))
            }
        }
    }

    PasteArea(viewModel = viewModel)
}

@Composable
private fun PasteArea(viewModel: ImportViewModel) {
    OutlinedTextField(
        value = viewModel.inputText,
        onValueChange = viewModel::onTextChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        minLines = 6,
        maxLines = 12,
        placeholder = { Text(stringResource(R.string.import_paste_hint)) },
    )
    if (viewModel.inputText.isBlank()) {
        Text(
            text = stringResource(R.string.import_empty_hint),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = viewModel::pasteFromClipboard) {
            Text(stringResource(R.string.import_from_clipboard))
        }
        OutlinedButton(onClick = viewModel::clearInput) {
            Text(stringResource(R.string.import_clear))
        }
        Button(
            onClick = viewModel::parse,
            enabled = viewModel.inputText.isNotBlank(),
        ) {
            Text(stringResource(R.string.import_parse))
        }
    }
}

@Composable
private fun GuideCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.import_guide_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(stringResource(R.string.import_step_1), style = MaterialTheme.typography.labelLarge)
            BulletLine(stringResource(R.string.import_step_1_tip1))
            BulletLine(stringResource(R.string.import_step_1_tip2))
            BulletLine(stringResource(R.string.import_step_1_tip3))
            Text(stringResource(R.string.import_step_2), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.import_step_3), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.import_step_4), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Text(
        text = "· $text",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 12.dp),
    )
}

private fun readTextFromUri(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()
