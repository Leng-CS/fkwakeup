package com.lengcs.fkwakeup.feature.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.CurrentTermProvider
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.designsystem.palette.CoursePalette
import com.lengcs.fkwakeup.core.exporter.TimetableExporter
import com.lengcs.fkwakeup.core.model.CourseWithSessions
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import com.lengcs.fkwakeup.widget.glance.WidgetRefreshScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CourseManageViewModel @Inject constructor(
    private val termRepository: TermRepository,
    private val courseRepository: CourseRepository,
    private val currentTermProvider: CurrentTermProvider,
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _term = MutableStateFlow<Term?>(null)
    val term: StateFlow<Term?> = _term

    private val _courses = MutableStateFlow<List<CourseWithSessions>>(emptyList())
    val courses: StateFlow<List<CourseWithSessions>> = _courses

    private val _sections = MutableStateFlow<List<SectionTemplate>>(emptyList())
    val sections: StateFlow<List<SectionTemplate>> = _sections

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            // 当前学期是流：切学期后自动重新订阅课程与节次表，无需重启应用
            currentTermProvider.observeCurrentTerm()
                .flatMapLatest { term ->
                    if (term == null) {
                        flowOf(ManageData())
                    } else {
                        combine(
                            courseRepository.observeCourses(term.id),
                            termRepository.observeSections(term.id),
                        ) { courses, sections ->
                            ManageData(term, courses, sections)
                        }
                    }
                }
                .collect { data ->
                    _term.value = data.term
                    _courses.value = data.courses
                    _sections.value = data.sections
                }
        }
    }

    private data class ManageData(
        val term: Term? = null,
        val courses: List<CourseWithSessions> = emptyList(),
        val sections: List<SectionTemplate> = emptyList(),
    )

    /** 生成可分享的课表 JSON；没有学期时返回 null */
    fun buildExportJson(): String? {
        val term = _term.value ?: return null
        return TimetableExporter.export(term, _sections.value, _courses.value)
    }

    fun deleteCourse(course: CourseWithSessions) {
        viewModelScope.launch {
            courseRepository.deleteCourse(course.course)
            WidgetRefreshScheduler.refreshNow(appContext)
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
    onManageWidgets: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseManageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                navigationIcon = { TextButton(onClick = onBack) { Text("‹") } },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = term?.name ?: stringResource(R.string.course_manage_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.course_overview, courses.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    )
                }
            }

            Text(
                text = stringResource(R.string.course_tools),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 18.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToolButton(
                    text = stringResource(R.string.course_export),
                    onClick = { exportTimetable(context, viewModel, scope, snackbarHostState) },
                    modifier = Modifier.weight(1f),
                )
                ToolButton(
                    text = stringResource(R.string.term_manage_title),
                    onClick = onManageTerms,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToolButton(
                    text = stringResource(R.string.sections_title),
                    onClick = onManageSections,
                    modifier = Modifier.weight(1f),
                )
                ToolButton(
                    text = stringResource(R.string.course_widgets),
                    onClick = onManageWidgets,
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = onAddCourse,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.course_add))
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
private fun ToolButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Text(text = text, maxLines = 1)
    }
}

/** 通过系统分享面板输出课表 JSON */
private fun exportTimetable(
    context: android.content.Context,
    viewModel: CourseManageViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
) {
    val json = viewModel.buildExportJson()
    if (json == null) {
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.export_empty))
        }
        return
    }

    val subject = context.getString(R.string.export_subject, viewModel.term.value?.name.orEmpty())
    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(android.content.Intent.EXTRA_TEXT, json)
        putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
    }
    context.startActivity(
        android.content.Intent.createChooser(sendIntent, context.getString(R.string.export_chooser_title)),
    )
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
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(12.dp)
                    .background(
                        color = CoursePalette.resolve(
                            customArgb = entry.course.colorArgb,
                            name = entry.course.name,
                        ),
                        shape = CircleShape,
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.course.name, style = MaterialTheme.typography.bodyLarge)
                entry.course.teacher?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = if (entry.onlineWindows.isEmpty()) {
                        stringResource(R.string.course_session_count, entry.sessions.size)
                    } else {
                        stringResource(
                            R.string.course_schedule_count,
                            entry.sessions.size,
                            entry.onlineWindows.size,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.edit_delete)) }
        }
    }
}
