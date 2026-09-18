package com.lengcs.fkwakeup.feature.importexport

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.importer.model.MergedCourse
import com.lengcs.fkwakeup.core.importer.model.SessionDraft

private val WEEK_NAMES = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun ImportPreviewScreen(
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
            text = stringResource(R.string.preview_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(
                R.string.preview_summary,
                viewModel.sessionCount,
                viewModel.courses.size,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )

        TermCard(viewModel)

        viewModel.courses.forEachIndexed { index, course ->
            CourseCard(
                index = index,
                course = course,
                onRename = { name, teacher -> viewModel.renameCourse(index, name, teacher) },
                onDelete = { viewModel.deleteCourse(index) },
                onSplitSession = { sessionIndex -> viewModel.splitSession(index, sessionIndex) },
            )
        }

        Button(
            onClick = viewModel::confirmImport,
            enabled = !viewModel.isImporting && viewModel.courses.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (viewModel.isImporting) stringResource(R.string.preview_importing) else stringResource(R.string.preview_confirm))
        }
        OutlinedButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_fail_retry))
        }
    }
}

@Composable
private fun TermCard(viewModel: ImportViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.preview_term_card), style = MaterialTheme.typography.titleSmall)
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
            viewModel.startDate?.let {
                Text("起始日：$it（已对齐到周一）", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CourseCard(
    index: Int,
    course: MergedCourse,
    onRename: (String, String?) -> Unit,
    onDelete: () -> Unit,
    onSplitSession: (Int) -> Unit,
) {
    var name by remember(course.name) { mutableStateOf(course.name) }
    var teacher by remember(course.teacher) { mutableStateOf(course.teacher.orEmpty()) }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onRename(it, teacher.ifBlank { null })
                },
                label = { Text("课程名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = teacher,
                onValueChange = {
                    teacher = it
                    onRename(name, it.ifBlank { null })
                },
                label = { Text("教师") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (course.sessions.isEmpty()) {
                Text(stringResource(R.string.preview_no_session), style = MaterialTheme.typography.bodySmall)
            }
            course.sessions.forEachIndexed { sessionIndex, session ->
                SessionRow(
                    session = session,
                    onSplit = { onSplitSession(sessionIndex) },
                )
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
    onSplit: () -> Unit,
) {
    Column {
        Text(
            text = buildString {
                append("周").append(WEEK_NAMES.getOrElse((session.dayOfWeek ?: 1) - 1) { "?" })
                append(" 第").append(session.startSection ?: 0).append("-").append(session.endSection ?: 0).append("节")
                append(" · ").append(session.weeks.orEmpty()).append("周")
                append(" · ").append(session.location ?: stringResource(R.string.preview_location_unknown))
                session.note?.let { append(" · ").append(it) }
            },
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onSplit) {
            Text(stringResource(R.string.preview_split))
        }
    }
}
