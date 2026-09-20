package com.lengcs.fkwakeup.feature.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lengcs.fkwakeup.core.common.WeekSpecParser
import com.lengcs.fkwakeup.core.designsystem.editor.CourseSessionEditForm
import com.lengcs.fkwakeup.core.designsystem.editor.CourseSessionEditState
import com.lengcs.fkwakeup.core.designsystem.editor.SheetTopBar
import com.lengcs.fkwakeup.core.importer.model.MergedCourse
import com.lengcs.fkwakeup.core.importer.model.SessionDraft

/**
 * 导入预览页的时间段编辑抽屉。
 *
 * 与周视图抽屉共用 [CourseSessionEditForm]，差别有两点：
 * 1. 数据源是**未落库**的 `MergedCourse` / `SessionDraft`，不是 DB 实体
 * 2. **没有「删除本节课」** —— 预览页的删除入口在课程卡片上（见 #21）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditSheet(
    course: MergedCourse,
    draft: SessionDraft,
    totalWeeks: Int,
    sectionCount: Int,
    onDismiss: () -> Unit,
    onSave: (CourseSessionEditState) -> Unit,
) {
    var state by remember(course.name, draft.index) {
        mutableStateOf(
            CourseSessionEditState(
                name = course.name,
                teacher = draft.teacher.orEmpty(),
                colorArgb = course.colorArgb,
                dayOfWeek = draft.dayOfWeek ?: 1,
                startSection = draft.startSection ?: 1,
                endSection = draft.endSection ?: (draft.startSection ?: 1) + 1,
                // AI 没给周次（null）或给了解析不了的串时，默认整学期 ——
                // 这与落库时的兜底一致，免得用户还要手动点一遍
                weeks = draft.weeks?.let { WeekSpecParser.parseOrNull(it, totalWeeks) }
                    ?: (1..totalWeeks).toSet(),
                location = draft.location.orEmpty(),
                note = draft.note.orEmpty(),
            ),
        )
    }
    var weeksError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetTopBar(
                title = stringResource(R.string.preview_edit_title),
                onSave = {
                    // #23：空周次会写出空 weekSpec，课程将在周视图里永久不显示
                    weeksError = state.weeks.isEmpty()
                    if (!weeksError) onSave(state)
                },
                saveEnabled = state.name.isNotBlank(),
                onCancel = onDismiss,
            )

            if (state.name.isBlank()) {
                Text(
                    text = stringResource(R.string.preview_name_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (weeksError) {
                Text(
                    text = stringResource(R.string.preview_weeks_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            CourseSessionEditForm(
                state = state,
                onStateChange = { state = it },
                totalWeeks = totalWeeks,
                sectionCount = sectionCount,
            )
        }
    }
}
