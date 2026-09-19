package com.lengcs.fkwakeup.feature.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import com.lengcs.fkwakeup.core.model.Term
import com.lengcs.fkwakeup.widget.glance.WidgetRefreshScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class TermManageViewModel @Inject constructor(
    private val termRepository: TermRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _terms = MutableStateFlow<List<Term>>(emptyList())
    val terms: StateFlow<List<Term>> = _terms

    private val _currentTermId = MutableStateFlow(-1L)
    val currentTermId: StateFlow<Long> = _currentTermId

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            termRepository.observeTerms(includeArchived = true).collect { _terms.value = it }
        }
        viewModelScope.launch {
            // 持续跟随，而不是只读一次 —— 否则删除当前学期后「当前」标记不会更新
            settingsRepository.settings
                .map { it.currentTermId }
                .distinctUntilChanged()
                .collect { _currentTermId.value = it }
        }
    }

    fun createTerm(name: String, startText: String, weeks: Int) {
        viewModelScope.launch {
            val date = runCatching { LocalDate.parse(startText.trim()) }.getOrNull()
            if (date == null) {
                _messages.trySend("日期格式应为 YYYY-MM-DD")
                return@launch
            }
            val id = termRepository.createTerm(
                name = name.trim().ifBlank { "未命名学期" },
                startMonday = date,
                totalWeeks = weeks.coerceIn(1, 30),
            )
            settingsRepository.setCurrentTerm(id)
            _currentTermId.value = id
            _messages.trySend("已创建学期")
        }
    }

    fun rename(term: Term, newName: String) {
        viewModelScope.launch {
            if (newName.isBlank()) return@launch
            termRepository.updateTerm(term.copy(name = newName.trim()))
        }
    }

    fun setArchived(term: Term, archived: Boolean) {
        viewModelScope.launch { termRepository.setArchived(term.id, archived) }
    }

    fun delete(term: Term) {
        viewModelScope.launch {
            termRepository.deleteTerm(term)
            if (_currentTermId.value == term.id) {
                val next = termRepository.observeTerms().first().firstOrNull()?.id ?: -1L
                settingsRepository.setCurrentTerm(next)
                _currentTermId.value = next
            }
            _messages.trySend("已删除学期")
        }
    }

    fun switchTo(term: Term) {
        viewModelScope.launch {
            settingsRepository.setCurrentTerm(term.id)
            _currentTermId.value = term.id
            // 小组件也跟着切，否则桌面上还停留在上个学期
            WidgetRefreshScheduler.refreshNow(appContext)
            _messages.trySend("已切换到「${term.name}」")
        }
    }

    fun validateDate(text: String): Boolean =
        try {
            LocalDate.parse(text.trim())
            true
        } catch (e: DateTimeParseException) {
            false
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TermManageViewModel = hiltViewModel(),
) {
    val terms by viewModel.terms.collectAsState()
    val currentTermId by viewModel.currentTermId.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreate by remember { mutableStateOf(false) }
    var renamingTerm by remember { mutableStateOf<Term?>(null) }
    var deletingTerm by remember { mutableStateOf<Term?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.term_manage_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            OutlinedButton(
                onClick = { showCreate = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.term_add))
            }

            if (terms.isEmpty()) {
                Text(
                    text = stringResource(R.string.term_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(terms, key = { it.id }) { term ->
                    TermRow(
                        term = term,
                        isCurrent = term.id == currentTermId,
                        onSwitch = { viewModel.switchTo(term) },
                        onRename = { renamingTerm = term },
                        onToggleArchive = { viewModel.setArchived(term, !term.isArchived) },
                        onDelete = { deletingTerm = term },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateTermDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, date, weeks ->
                viewModel.createTerm(name, date, weeks)
                showCreate = false
            },
        )
    }

    renamingTerm?.let { term ->
        var text by remember { mutableStateOf(term.name) }
        AlertDialog(
            onDismissRequest = { renamingTerm = null },
            title = { Text(stringResource(R.string.term_rename)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.term_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rename(term, text)
                    renamingTerm = null
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { renamingTerm = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    deletingTerm?.let { term ->
        AlertDialog(
            onDismissRequest = { deletingTerm = null },
            title = { Text(stringResource(R.string.term_delete)) },
            text = { Text(stringResource(R.string.term_delete_confirm, term.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(term)
                    deletingTerm = null
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTerm = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TermRow(
    term: Term,
    isCurrent: Boolean,
    onSwitch: () -> Unit,
    onRename: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onSwitch),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(term.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${term.startMonday} · ${term.totalWeeks} 周" +
                            if (term.isArchived) " · ${stringResource(R.string.term_archived)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (isCurrent) {
                    Text(
                        text = stringResource(R.string.term_current),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row {
                TextButton(onClick = onRename) { Text(stringResource(R.string.term_rename)) }
                TextButton(onClick = onToggleArchive) {
                    Text(
                        if (term.isArchived) {
                            stringResource(R.string.term_unarchive)
                        } else {
                            stringResource(R.string.term_archive)
                        },
                    )
                }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.term_delete)) }
            }
        }
    }
}

@Composable
private fun CreateTermDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var weeks by remember { mutableStateOf("18") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.term_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.term_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text(stringResource(R.string.term_start_date)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = weeks,
                    onValueChange = { weeks = it },
                    label = { Text(stringResource(R.string.term_weeks)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onCreate(name, date, weeks.toIntOrNull() ?: 18)
            }) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
