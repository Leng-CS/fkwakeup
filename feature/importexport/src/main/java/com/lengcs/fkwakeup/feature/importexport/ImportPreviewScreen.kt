package com.lengcs.fkwakeup.feature.importexport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.common.CourseColorPalette
import com.lengcs.fkwakeup.core.importer.model.MergedCourse
import com.lengcs.fkwakeup.core.importer.model.OnlineWindowDraft
import com.lengcs.fkwakeup.core.importer.model.SessionDraft
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode

private val WEEK_NAMES = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 确认识别结果页。
 *
 * 课程卡片**只读**展示（课名 / 教师 / 颜色 / 时间段列表），
 * 点某条时间段行弹出编辑抽屉改全部字段 —— 见 #21。
 */
@Composable
fun ImportPreviewScreen(
    viewModel: ImportViewModel,
    modifier: Modifier = Modifier,
) {
    // 正在编辑的 (课程下标, 时间段下标)
    var editingCourse by remember { mutableStateOf<Int?>(null) }
    var editingSession by remember { mutableStateOf<Int?>(null) }
    var editingWindowCourse by remember { mutableStateOf<Int?>(null) }
    var editingWindow by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.preview_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(
                R.string.preview_summary,
                viewModel.sessionCount,
                viewModel.onlineWindowCount,
                viewModel.courses.size,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.preview_tap_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TermCard(viewModel)

        viewModel.courses.forEachIndexed { index, course ->
            CourseCard(
                course = course,
                onEditSession = { sessionIndex ->
                    editingCourse = index
                    editingSession = sessionIndex
                },
                onEditWindow = { windowIndex ->
                    editingWindowCourse = index
                    editingWindow = windowIndex
                },
                onDelete = { viewModel.deleteCourse(index) },
                onSplitSession = { sessionIndex -> viewModel.splitSession(index, sessionIndex) },
            )
        }

        viewModel.pendingImportProblems.distinct().forEach { problem ->
            Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = viewModel::confirmImport,
            enabled = !viewModel.isImporting && viewModel.courses.isNotEmpty() && viewModel.pendingImportProblems.isEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (viewModel.isImporting) {
                    stringResource(R.string.preview_importing)
                } else {
                    stringResource(R.string.preview_confirm)
                },
            )
        }
        OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_fail_retry))
        }
    }

    // ---- 时间段编辑抽屉 ----
    val courseIndex = editingCourse
    val sessionIndex = editingSession
    if (courseIndex != null && sessionIndex != null) {
        val course = viewModel.courses.getOrNull(courseIndex)
        val draft = course?.sessions?.getOrNull(sessionIndex)
        if (course != null && draft != null) {
            SessionEditSheet(
                course = course,
                draft = draft,
                totalWeeks = viewModel.totalWeeks,
                sectionCount = viewModel.defaultSectionCount(),
                onConvertToAsync = {
                    viewModel.convertSessionToWindow(courseIndex, sessionIndex)
                    editingCourse = null
                    editingSession = null
                },
                onDismiss = {
                    editingCourse = null
                    editingSession = null
                },
                onSave = { state, mode, platform, url ->
                    // 课程级字段（作用于该课全部时间段）
                    viewModel.renameCourse(courseIndex, state.name, state.teacher.ifBlank { null })
                    viewModel.updateCourseColor(courseIndex, state.colorArgb)
                    // 时间段级字段（只作用于这一条）
                    viewModel.updateSession(
                        courseIndex = courseIndex,
                        sessionIndex = sessionIndex,
                        dayOfWeek = state.dayOfWeek,
                        startSection = state.startSection,
                        endSection = state.endSection,
                        weeks = state.weeks,
                        location = state.location,
                        note = state.note,
                        deliveryMode = mode,
                        onlinePlatform = platform,
                        onlineUrl = url,
                    )
                    editingCourse = null
                    editingSession = null
                },
            )
        }
    }
    val windowCourseIndex = editingWindowCourse
    val windowIndex = editingWindow
    if (windowCourseIndex != null && windowIndex != null) {
        val windowCourse = viewModel.courses.getOrNull(windowCourseIndex)
        val window = windowCourse?.onlineWindows?.getOrNull(windowIndex)
        if (windowCourse != null && window != null) {
            OnlineWindowEditSheet(
                courseName = windowCourse.name,
                courseTeacher = windowCourse.teacher,
                window = window,
                onSave = { name, teacher, updatedWindow ->
                    viewModel.renameCourse(windowCourseIndex, name, teacher)
                    viewModel.updateOnlineWindow(windowCourseIndex, windowIndex, updatedWindow)
                    editingWindowCourse = null
                    editingWindow = null
                },
                onConvertToLive = {
                    viewModel.convertWindowToLive(windowCourseIndex, windowIndex)
                    editingWindowCourse = null
                    editingWindow = null
                },
                onDismiss = {
                    editingWindowCourse = null
                    editingWindow = null
                },
            )
        }
    }
    if (viewModel.conflicts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConflicts,
            title = { Text("发现课程时间冲突") },
            text = { Text(viewModel.conflicts.take(3).joinToString("\n") { it.message() } + if (viewModel.conflicts.size > 3) "\n另有 ${viewModel.conflicts.size - 3} 项" else "") },
            confirmButton = { TextButton(onClick = { viewModel.confirmImport(force = true) }) { Text("仍然导入") } },
            dismissButton = { TextButton(onClick = viewModel::dismissConflicts) { Text("返回修改") } },
        )
    }
}

@Composable
private fun TermCard(viewModel: ImportViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.preview_term_card),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = viewModel.termName,
                onValueChange = viewModel::onTermNameChanged,
                label = { Text(stringResource(R.string.preview_term_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.totalWeeks.toString(),
                onValueChange = { raw ->
                    raw.toIntOrNull()?.let { viewModel.onTotalWeeksChanged(it) }
                },
                label = { Text(stringResource(R.string.preview_term_weeks)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            StartDateField(
                dateText = viewModel.startDate?.toString(),
                onPick = viewModel::onStartDateChanged,
            )
        }
    }
}

@Composable
private fun CourseCard(
    course: MergedCourse,
    onEditSession: (Int) -> Unit,
    onEditWindow: (Int) -> Unit,
    onDelete: () -> Unit,
    onSplitSession: (Int) -> Unit,
) {
    val colorArgb = CourseColorPalette.resolve(course.colorArgb, course.name)

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color(colorArgb), CircleShape),
                )
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            course.teacher?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (course.sessions.isEmpty() && course.onlineWindows.isEmpty()) {
                Text(
                    stringResource(R.string.preview_no_session),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            course.sessions.forEachIndexed { sessionIndex, session ->
                SessionRow(
                    session = session,
                    onClick = { onEditSession(sessionIndex) },
                )
                // 最后一条之后不用分隔线
                if (sessionIndex != course.sessions.lastIndex) {
                    HorizontalDivider()
                }
            }

            course.onlineWindows.forEachIndexed { index, window ->
                OnlineWindowRow(window, onClick = { onEditWindow(index) })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.preview_delete))
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: SessionDraft,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = buildString {
                if (session.deliveryMode == SessionDeliveryMode.LIVE_ONLINE) {
                    append(stringResource(R.string.preview_live_online)).append(" · ")
                }
                append("周").append(session.dayLabel())
                append(" 第")
                    .append(session.startSection ?: 0)
                    .append("-")
                    .append(session.endSection ?: 0)
                    .append("节")
                append(" · ").append(session.weeks.orEmpty()).append("周")
                append(" · ").append(session.location ?: stringResource(R.string.preview_location_unknown))
                session.onlinePlatform?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                session.note?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(R.string.preview_edit_session),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun OnlineWindowRow(window: OnlineWindowDraft, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp)) {
        Text(
            text = stringResource(
                R.string.preview_online_window,
                window.startDate?.toString() ?: stringResource(R.string.preview_online_missing),
                window.endDate?.toString() ?: stringResource(R.string.preview_online_missing),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        listOfNotNull(window.platform, window.url, window.note)
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let {
                Text(
                    text = it.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

private fun SessionDraft.dayLabel(): String {
    val day = dayOfWeek
    return if (day == null || day !in 1..7) "?" else WEEK_NAMES[day - 1]
}
