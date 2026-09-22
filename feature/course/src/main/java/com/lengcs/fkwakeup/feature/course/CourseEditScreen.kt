package com.lengcs.fkwakeup.feature.course

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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.core.designsystem.R as DsR
import com.lengcs.fkwakeup.core.common.WeekSpecFormatter
import com.lengcs.fkwakeup.core.common.WeekSpecParser
import com.lengcs.fkwakeup.core.common.OnlineCourseTimeline
import com.lengcs.fkwakeup.core.designsystem.picker.LabeledWheel
import com.lengcs.fkwakeup.core.designsystem.picker.WeekPicker
import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.CurrentTermProvider
import com.lengcs.fkwakeup.core.model.Course
import com.lengcs.fkwakeup.core.model.CourseSession
import com.lengcs.fkwakeup.core.model.OnlineCourseWindow
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.Term
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate

/**
 * 可编辑的时间段。
 *
 * 周次用**集合**表示（UI 是点选），保存时再由 `WeekSpecFormatter` 转成表达式。
 */
data class EditableSession(
    val dayOfWeek: Int = 1,
    val startSection: Int = 1,
    val endSection: Int = 2,
    val weeks: Set<Int> = emptySet(),
    val location: String? = null,
    val note: String? = null,
    val deliveryMode: SessionDeliveryMode = SessionDeliveryMode.ONSITE,
    val onlinePlatform: String? = null,
    val onlineUrl: String? = null,
)

@HiltViewModel
class CourseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
    private val termRepository: com.lengcs.fkwakeup.core.database.repository.TermRepository,
    private val currentTermProvider: CurrentTermProvider,
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val courseId: Long = savedStateHandle.get<String>("courseId")?.toLongOrNull() ?: 0L

    private val _term = MutableStateFlow<Term?>(null)
    val term: StateFlow<Term?> = _term

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private val _initial = MutableStateFlow<CourseEditInitial?>(null)
    val initial: StateFlow<CourseEditInitial?> = _initial

    private val _sections = MutableStateFlow<List<SectionTemplate>>(emptyList())
    val sections: StateFlow<List<SectionTemplate>> = _sections

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val isNew: Boolean get() = courseId == 0L

    data class CourseEditInitial(
        val name: String,
        val teacher: String,
        val note: String,
        val sessions: List<EditableSession>,
        val onlineWindows: List<EditableOnlineWindow>,
    )

    init {
        viewModelScope.launch {
            // 必须是「当前学期」，**不能**用 termRepository.observeTerms().first()。
            // 后者拿到的是「开学日期最晚的学期」（DAO 按 start_monday_epoch_day DESC 排序），
            // 多学期时它与当前学期很可能不是同一个 —— 于是：
            //   · 新建课程被写进那个学期 → 当前学期的管理页里看不到（#26）
            //   · 编辑已有课程会把它的 term_id 改走 → 课程从当前学期彻底消失（#26）
            // 「当前学期」的唯一权威来源是 CurrentTermProvider。
            val term = currentTermProvider.observeCurrentTerm().first()
            _term.value = term
            // 节次表用于滚轮的取值范围（1..节次数）
            _sections.value = term?.let { termRepository.getSections(it.id) } ?: emptyList()
            if (courseId == 0L) {
                _initial.value = CourseEditInitial("", "", "", emptyList(), emptyList())
            } else {
                val entry = courseRepository.getCourseWithSessions(courseId)
                if (entry == null) {
                    _initial.value = CourseEditInitial("", "", "", emptyList(), emptyList())
                } else {
                    _initial.value = CourseEditInitial(
                        name = entry.course.name,
                        teacher = entry.course.teacher.orEmpty(),
                        note = entry.course.note.orEmpty(),
                        sessions = entry.sessions.map {
                            EditableSession(
                                dayOfWeek = it.dayOfWeek,
                                startSection = it.startSection,
                                endSection = it.endSection,
                                weeks = WeekSpecParser.parseOrNull(it.weekSpec, term?.totalWeeks ?: 18)
                                    ?: emptySet(),
                                location = it.location,
                                note = it.note,
                                deliveryMode = it.deliveryMode,
                                onlinePlatform = it.onlinePlatform,
                                onlineUrl = it.onlineUrl,
                            )
                        },
                        onlineWindows = entry.onlineWindows.map { EditableOnlineWindow.fromDomain(it) },
                    )
                }
            }
            _loaded.value = true
        }
    }

    fun save(
        name: String,
        teacher: String,
        note: String,
        sessions: List<EditableSession>,
        onlineWindows: List<EditableOnlineWindow>,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            // 没有当前学期时**不能静默 return** —— 用户点了保存却什么都不发生，
            // 会以为已经保存成功（这条路径以前就是静默失败）
            val term = _term.value
            val termId = term?.id
            if (termId == null) {
                _messages.trySend("还没有学期，请先在「学期管理」里新建一个")
                return@launch
            }
            if (name.isBlank()) {
                _messages.trySend("课程名称不能为空")
                return@launch
            }

            val invalidDates = onlineWindows.firstOrNull { it.endDate.isBefore(it.startDate) }
            if (invalidDates != null) {
                _messages.trySend("网课结束日期不能早于开始日期")
                return@launch
            }
            val outsideTerm = onlineWindows.firstOrNull {
                !OnlineCourseTimeline.overlapsTerm(it.toDomain(courseId.coerceAtLeast(0L)), term)
            }
            if (outsideTerm != null) {
                _messages.trySend("网课开放期必须与当前学期至少重叠一天")
                return@launch
            }

            val totalWeeks = _term.value?.totalWeeks ?: 18
            // 没选周次的时间段直接跳过，避免写坏数据导致周视图崩溃
            val valid = sessions.filter { it.weeks.isNotEmpty() }
            if (valid.size != sessions.size) {
                _messages.trySend("有 ${sessions.size - valid.size} 个时间段未选择周次，已跳过")
            }

            val course = Course(
                id = courseId,
                termId = termId,
                name = name.trim(),
                teacher = teacher.trim().ifBlank { null },
                note = note.trim().ifBlank { null },
            )

            val targetId = if (isNew) {
                courseRepository.addCourse(course)
            } else {
                courseRepository.updateCourse(course)
                courseId
            }

            courseRepository.replaceSessions(
                targetId,
                valid.map {
                    CourseSession(
                        courseId = targetId,
                        dayOfWeek = it.dayOfWeek,
                        startSection = it.startSection,
                        endSection = it.endSection,
                        weekSpec = WeekSpecFormatter.format(it.weeks, totalWeeks),
                        location = it.location,
                        note = it.note,
                        deliveryMode = it.deliveryMode,
                        onlinePlatform = if (it.deliveryMode == SessionDeliveryMode.LIVE_ONLINE) it.onlinePlatform else null,
                        onlineUrl = if (it.deliveryMode == SessionDeliveryMode.LIVE_ONLINE) it.onlineUrl else null,
                    )
                },
            )
            courseRepository.replaceOnlineWindows(
                targetId,
                onlineWindows.map { it.toDomain(targetId) },
            )
            // 写库后主动刷新小组件，否则要等 15 分钟兜底
            com.lengcs.fkwakeup.widget.glance.WidgetRefreshScheduler.refreshNow(appContext)
            _messages.trySend("已保存")
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            if (!isNew) {
                courseRepository.getCourse(courseId)?.let { courseRepository.deleteCourse(it) }
                _messages.trySend("已删除")
            }
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseEditViewModel = hiltViewModel(),
) {
    val loaded by viewModel.loaded.collectAsState()
    val initial by viewModel.initial.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // 先取到局部变量再判空，委托属性没法智能转换成非空
    val initialValue = initial
    if (!loaded || initialValue == null) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_title_existing)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
            )
        }) { padding ->
            Text("加载中…", modifier = Modifier.padding(padding).padding(16.dp))
        }
        return
    }

    val start = initialValue
    var name by remember { mutableStateOf(TextFieldValue(start.name)) }
    var teacher by remember { mutableStateOf(TextFieldValue(start.teacher)) }
    var note by remember { mutableStateOf(TextFieldValue(start.note)) }
    val sessions = remember { mutableStateListOf<EditableSession>().apply { addAll(start.sessions) } }
    val onlineWindows = remember {
        mutableStateListOf<EditableOnlineWindow>().apply { addAll(start.onlineWindows) }
    }

    val totalWeeks by viewModel.term.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val totalWeekCount = totalWeeks?.totalWeeks ?: 18
    val sectionCount = sections.size.coerceAtLeast(1)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isNew) {
                            stringResource(R.string.edit_title_new)
                        } else {
                            stringResource(R.string.edit_title_existing)
                        },
                    )
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.edit_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = { Text(stringResource(R.string.edit_teacher)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.edit_note)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.edit_sessions), style = MaterialTheme.typography.titleSmall)
            if (sessions.isEmpty()) {
                Text(
                    stringResource(R.string.edit_no_session),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            sessions.forEachIndexed { index, session ->
                SessionEditor(
                    index = index,
                    session = session,
                    totalWeeks = totalWeekCount,
                    sectionCount = sectionCount,
                    onChange = { sessions[index] = it },
                    onRemove = { sessions.removeAt(index) },
                )
            }

            OutlinedButton(
                onClick = { sessions.add(EditableSession()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.edit_add_session))
            }

            Text(stringResource(R.string.edit_online_windows), style = MaterialTheme.typography.titleSmall)
            if (onlineWindows.isEmpty()) {
                Text(stringResource(R.string.edit_no_online_window), style = MaterialTheme.typography.bodySmall)
            }
            onlineWindows.forEachIndexed { index, window ->
                OnlineWindowEditor(
                    index = index,
                    window = window,
                    term = totalWeeks,
                    onChange = { onlineWindows[index] = it },
                    onRemove = { onlineWindows.removeAt(index) },
                )
            }
            OutlinedButton(
                onClick = {
                    val term = totalWeeks ?: return@OutlinedButton
                    onlineWindows.add(
                        EditableOnlineWindow(
                            startDate = term.startMonday,
                            endDate = OnlineCourseTimeline.termEnd(term),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.edit_add_online_window))
            }

            Button(
                onClick = {
                    viewModel.save(
                        name.text,
                        teacher.text,
                        note.text,
                        sessions.toList(),
                        onlineWindows.toList(),
                        onBack,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.edit_save))
            }

            if (!viewModel.isNew) {
                OutlinedButton(
                    onClick = { viewModel.delete(onBack) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.edit_delete))
                }
            }
        }
    }
}

private fun sectionItems(sectionCount: Int): List<String> =
    (1..sectionCount.coerceAtLeast(1)).map { "$it" }

private val WEEKDAY_ITEMS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@Composable
private fun SessionEditor(
    index: Int,
    session: EditableSession,
    totalWeeks: Int,
    sectionCount: Int,
    onChange: (EditableSession) -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = session.deliveryMode == SessionDeliveryMode.ONSITE,
                    onClick = {
                        onChange(
                            session.copy(
                                deliveryMode = SessionDeliveryMode.ONSITE,
                                onlinePlatform = null,
                                onlineUrl = null,
                            ),
                        )
                    },
                    label = { Text(stringResource(R.string.edit_mode_onsite)) },
                )
                FilterChip(
                    selected = session.deliveryMode == SessionDeliveryMode.LIVE_ONLINE,
                    onClick = { onChange(session.copy(deliveryMode = SessionDeliveryMode.LIVE_ONLINE)) },
                    label = { Text(stringResource(R.string.edit_mode_live)) },
                )
            }
            // 与周视图编辑抽屉保持一致：滚轮选周几 / 起止节次
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LabeledWheel(
                    label = stringResource(R.string.edit_day),
                    items = WEEKDAY_ITEMS,
                    selectedIndex = (session.dayOfWeek - 1).coerceIn(0, 6),
                    onSelectedChange = { onChange(session.copy(dayOfWeek = it + 1)) },
                    modifier = Modifier.weight(1f),
                )
                LabeledWheel(
                    label = "起",
                    items = sectionItems(sectionCount),
                    selectedIndex = (session.startSection - 1).coerceIn(0, sectionCount - 1),
                    onSelectedChange = { onChange(session.copy(startSection = it + 1)) },
                    modifier = Modifier.weight(1f),
                )
                LabeledWheel(
                    label = "止",
                    items = sectionItems(sectionCount),
                    selectedIndex = (session.endSection - 1).coerceIn(0, sectionCount - 1),
                    onSelectedChange = { onChange(session.copy(endSection = it + 1)) },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(stringResource(R.string.edit_weeks), style = MaterialTheme.typography.labelLarge)
            WeekPicker(
                selectedWeeks = session.weeks,
                totalWeeks = totalWeeks,
                onToggleWeek = { week ->
                    val next = if (week in session.weeks) {
                        session.weeks - week
                    } else {
                        session.weeks + week
                    }
                    onChange(session.copy(weeks = next))
                },
                onSelectAll = { onChange(session.copy(weeks = (1..totalWeeks).toSet())) },
                onSelectOdd = {
                    onChange(session.copy(weeks = (1..totalWeeks).filter { it % 2 == 1 }.toSet()))
                },
                onSelectEven = {
                    onChange(session.copy(weeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet()))
                },
                onClear = { onChange(session.copy(weeks = emptySet())) },
            )
            if (session.deliveryMode == SessionDeliveryMode.LIVE_ONLINE) {
                OutlinedTextField(
                    value = session.onlinePlatform.orEmpty(),
                    onValueChange = { onChange(session.copy(onlinePlatform = it.ifBlank { null })) },
                    label = { Text(stringResource(R.string.edit_online_platform)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = session.onlineUrl.orEmpty(),
                    onValueChange = { onChange(session.copy(onlineUrl = it.ifBlank { null })) },
                    label = { Text(stringResource(R.string.edit_online_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = session.location.orEmpty(),
                    onValueChange = { onChange(session.copy(location = it.ifBlank { null })) },
                    label = { Text(stringResource(R.string.edit_location)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "时间段 ${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRemove) { Text(stringResource(DsR.string.action_remove)) }
            }
        }
    }
}
