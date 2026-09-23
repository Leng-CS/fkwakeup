package com.lengcs.fkwakeup.feature.course

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.lengcs.fkwakeup.core.database.repository.CurrentTermProvider
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.model.DefaultSections
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import com.lengcs.fkwakeup.core.designsystem.R as DsR
import com.lengcs.fkwakeup.widget.glance.WidgetRefreshScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SectionTemplateViewModel @Inject constructor(
    private val termRepository: TermRepository,
    private val currentTermProvider: CurrentTermProvider,
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _term = MutableStateFlow<Term?>(null)
    private val _sections = MutableStateFlow<List<SectionTemplate>>(emptyList())
    private val _messages = Channel<String>(Channel.BUFFERED)

    val sections: StateFlow<List<SectionTemplate>> = _sections
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            val termId = currentTermProvider.resolveCurrentTermId() ?: return@launch
            _term.value = termRepository.getTerm(termId) ?: return@launch
            termRepository.observeSections(termId).collect { _sections.value = it }
        }
    }

    fun save(sections: List<SectionTemplate>, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val termId = _term.value?.id ?: run {
                onResult(false)
                return@launch
            }
            val ordered = sections.sortedBy(SectionTemplate::index)
            val invalid = ordered.firstOrNull { it.endMinutes <= it.startMinutes }
            if (invalid != null) {
                _messages.trySend("第 ${invalid.index} 节时间有误（结束需晚于开始）")
                onResult(false)
                return@launch
            }
            val overlap = ordered.zipWithNext().firstOrNull { (previous, next) ->
                previous.endMinutes > next.startMinutes
            }
            if (overlap != null) {
                _messages.trySend("第 ${overlap.first.index} 节结束时间不得晚于第 ${overlap.second.index} 节开始时间")
                onResult(false)
                return@launch
            }
            onResult(persistSections(termId, ordered, "已保存"))
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            val termId = _term.value?.id ?: return@launch
            persistSections(termId, DefaultSections.forTerm(termId), "已恢复默认")
        }
    }

    private suspend fun persistSections(termId: Long, sections: List<SectionTemplate>, success: String): Boolean {
        try {
            termRepository.replaceSections(termId, sections)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            _messages.trySend("保存失败，原时间表未更改，请重试")
            return false
        }
        _messages.trySend(success)
        try {
            WidgetRefreshScheduler.refreshNow(appContext)
            com.lengcs.fkwakeup.core.reminder.ReminderScheduler.requestRebuild(appContext)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            _messages.trySend("时间表已保存，小组件暂未刷新")
        }
        return true
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
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var draftSections by remember { mutableStateOf<List<SectionTemplate>?>(null) }
    var showLeavePrompt by remember { mutableStateOf(false) }
    var savingOnExit by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }

    val requestBack = {
        if (hasUnsavedChanges) showLeavePrompt = true else onBack()
    }
    BackHandler { if (!savingOnExit) requestBack() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sections_title)) },
                navigationIcon = { TextButton(onClick = requestBack) { Text("<") } },
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
                modifier = Modifier.padding(16.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            ) {
                item {
                    SectionTimingEditor(
                        sections = sections,
                        onSave = { viewModel.save(it) },
                        onReset = viewModel::resetToDefault,
                        onDraftChange = { dirty, candidate ->
                            hasUnsavedChanges = dirty
                            draftSections = candidate
                        },
                    )
                }
            }
        }
    }

    if (showLeavePrompt) {
        AlertDialog(
            onDismissRequest = { if (!savingOnExit) showLeavePrompt = false },
            title = { Text(stringResource(R.string.sections_unsaved_title)) },
            text = {
                Text(stringResource(
                    when {
                        saveFailed -> R.string.sections_unsaved_save_failed
                        draftSections == null -> R.string.sections_unsaved_invalid
                        else -> R.string.sections_unsaved_message
                    },
                ))
            },
            confirmButton = {
                TextButton(
                    enabled = draftSections != null && !savingOnExit,
                    onClick = {
                        val candidate = draftSections ?: return@TextButton
                        savingOnExit = true
                        saveFailed = false
                        viewModel.save(candidate) { saved ->
                            savingOnExit = false
                            if (saved) onBack() else saveFailed = true
                        }
                    },
                ) { Text(stringResource(DsR.string.action_save)) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showLeavePrompt = false }, enabled = !savingOnExit) {
                        Text(stringResource(R.string.sections_continue_editing))
                    }
                    TextButton(onClick = onBack, enabled = !savingOnExit) {
                        Text(stringResource(R.string.sections_discard_changes))
                    }
                }
            },
        )
    }
}
