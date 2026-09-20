package com.lengcs.fkwakeup.feature.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.core.database.repository.CurrentTermProvider
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.designsystem.R as DsR
import com.lengcs.fkwakeup.core.model.DefaultSections
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SectionTemplateViewModel @Inject constructor(
    private val termRepository: TermRepository,
    private val currentTermProvider: CurrentTermProvider,
) : ViewModel() {

    private val _term = MutableStateFlow<Term?>(null)
    val term: StateFlow<Term?> = _term

    private val _sections = MutableStateFlow<List<SectionTemplate>>(emptyList())
    val sections: StateFlow<List<SectionTemplate>> = _sections

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            // 节次表是「当前学期」的属性，必须走权威来源取当前学期 ——
            // 不要自己拼 `settings.currentTermId ?: observeTerms().first()`（见 CHANGELOG #26）
            val termId = currentTermProvider.resolveCurrentTermId() ?: return@launch
            val term = termRepository.getTerm(termId) ?: return@launch
            _term.value = term
            termRepository.observeSections(termId).collect { _sections.value = it }
        }
    }

    fun save(sections: List<SectionTemplate>) {
        viewModelScope.launch {
            val termId = _term.value?.id ?: return@launch
            val invalid = sections.firstOrNull { it.endMinutes <= it.startMinutes }
            if (invalid != null) {
                _messages.trySend("第 ${invalid.index} 节时间有误（结束需晚于开始）")
                return@launch
            }
            termRepository.replaceSections(termId, sections)
            _messages.trySend("已保存")
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            val termId = _term.value?.id ?: return@launch
            termRepository.replaceSections(termId, DefaultSections.forTerm(termId))
            _messages.trySend("已恢复默认")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionTemplateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SectionTemplateViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sections_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Text(
                text = stringResource(R.string.sections_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )

            // 节次时间用字符串编辑，保存时才解析 —— 避免边打字边解析产生抖动
            val rows = remember(sections) {
                mutableStateListOf<TimeRow>().apply {
                    clear()
                    addAll(
                        sections.map {
                            TimeRow(
                                index = it.index,
                                start = formatMinutes(it.startMinutes),
                                end = formatMinutes(it.endMinutes),
                            )
                        },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                itemsIndexed(rows, key = { _, row -> row.index }) { position, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${row.index}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(top = 18.dp)
                                .weight(0.3f),
                        )
                        TimeField(
                            value = rows[position].start,
                            onValueChange = { rows[position] = rows[position].copy(start = it) },
                            modifier = Modifier.weight(1f),
                        )
                        TimeField(
                            value = rows[position].end,
                            onValueChange = { rows[position] = rows[position].copy(end = it) },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { rows.removeAt(position) }) {
                            Text(stringResource(DsR.string.action_remove))
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val nextIndex = (rows.maxOfOrNull { it.index } ?: 0) + 1
                    rows.add(TimeRow(index = nextIndex, start = "08:00", end = "08:45"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.sections_add))
            }

            OutlinedButton(
                onClick = viewModel::resetToDefault,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.sections_reset))
            }

            Button(
                onClick = { viewModel.save(rows.mapNotNull { it.toSection() }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(stringResource(DsR.string.action_save))
            }
        }
    }
}

private data class TimeRow(
    val index: Int,
    val start: String,
    val end: String,
)

private fun TimeRow.toSection(): SectionTemplate? {
    val s = parseMinutes(start) ?: return null
    val e = parseMinutes(end) ?: return null
    return SectionTemplate(termId = 0L, index = index, startMinutes = s, endMinutes = e)
}

@Composable
private fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.sections_time_hint)) },
        singleLine = true,
        modifier = modifier,
    )
}

private fun formatMinutes(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)

private fun parseMinutes(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}
