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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import com.lengcs.fkwakeup.core.model.CourseWithSessions
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
class CourseManageViewModel @Inject constructor(
    private val termRepository: TermRepository,
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _term = MutableStateFlow<Term?>(null)
    val term: StateFlow<Term?> = _term

    private val _courses = MutableStateFlow<List<CourseWithSessions>>(emptyList())
    val courses: StateFlow<List<CourseWithSessions>> = _courses

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val termId = settings.currentTermId.takeIf { it > 0 }
                ?: termRepository.observeTerms().first().firstOrNull()?.id
            if (termId == null) return@launch
            val term = termRepository.getTerm(termId) ?: return@launch
            _term.value = term
            courseRepository.observeCourses(termId).collect { _courses.value = it }
        }
    }

    fun deleteCourse(course: CourseWithSessions) {
        viewModelScope.launch {
            courseRepository.deleteCourse(course.course)
            _messages.trySend("已删除「${course.course.name}」")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManageScreen(
    onBack: () -> Unit,
    onAddCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onManageTerms: () -> Unit,
    onManageSections: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseManageViewModel = hiltViewModel(),
) {
    val term by viewModel.term.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.course_manage_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
                actions = {
                    TextButton(onClick = onManageTerms) { Text(stringResource(R.string.term_manage_title)) }
                    TextButton(onClick = onManageSections) { Text(stringResource(R.string.sections_title)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            TextButton(
                onClick = onAddCourse,
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.course_add))
            }

            term?.let {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (courses.isEmpty()) {
                Text(
                    text = stringResource(R.string.course_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(courses, key = { it.course.id }) { entry ->
                    CourseRow(
                        entry = entry,
                        onClick = { onEditCourse(entry.course.id) },
                        onDelete = { viewModel.deleteCourse(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseRow(
    entry: CourseWithSessions,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.course.name, style = MaterialTheme.typography.bodyLarge)
                entry.course.teacher?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = stringResource(R.string.course_session_count, entry.sessions.size),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.edit_delete)) }
        }
    }
}
