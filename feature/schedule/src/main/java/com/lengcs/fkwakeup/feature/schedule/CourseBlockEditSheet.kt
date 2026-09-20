package com.lengcs.fkwakeup.feature.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.lengcs.fkwakeup.core.common.ScheduleBlock
import com.lengcs.fkwakeup.core.common.WeekSpecParser
// 表单文案在 core:designsystem（与导入预览抽屉共用）；操作/对话框文案仍在本模块。
// 两个模块都有资源，用别名区分，避免 R 被覆盖后指错模块。
import com.lengcs.fkwakeup.core.designsystem.R as DsR
import com.lengcs.fkwakeup.core.designsystem.editor.CourseSessionEditForm
import com.lengcs.fkwakeup.core.designsystem.editor.CourseSessionEditState
import com.lengcs.fkwakeup.core.designsystem.editor.SheetTopBar

/**
 * 周视图里点击课程块弹出的编辑抽屉。
 *
 * 表单主体由 `core:designsystem` 的 [CourseSessionEditForm] 提供，与导入预览页共用同一套字段与交互。
 * 本文件只负责两件事：把 DB 模型填进表单状态、把表单结果回传。
 *
 * 动作按钮在**右上角**（保存 / 删除 / 取消），与下方输入区分离。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseBlockEditSheet(
    block: ScheduleBlock,
    totalWeeks: Int,
    sectionCount: Int,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        teacher: String,
        colorArgb: Int?,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
        weeks: Set<Int>,
        location: String?,
        note: String?,
    ) -> Unit,
    onDelete: () -> Unit,
) {
    var state by remember(block.course.id, block.session.id) {
        mutableStateOf(
            CourseSessionEditState(
                name = block.course.name,
                teacher = block.course.teacher.orEmpty(),
                colorArgb = block.course.colorArgb,
                dayOfWeek = block.session.dayOfWeek,
                startSection = block.session.startSection,
                endSection = block.session.endSection,
                // 已有表达式反解成周集合；解析不出来就留空，由用户重新点选
                weeks = runCatching {
                    WeekSpecParser.parse(block.session.weekSpec, totalWeeks)
                }.getOrDefault(emptySet()),
                location = block.session.location.orEmpty(),
                note = block.session.note.orEmpty(),
            ),
        )
    }
    var confirmDelete by remember { mutableStateOf(false) }
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
                title = stringResource(DsR.string.sheet_title),
                onSave = {
                    if (state.weeks.isEmpty()) {
                        // #23：空周次会写出空 weekSpec，该课程会在周视图里永久不显示
                        weeksError = true
                    } else {
                        weeksError = false
                        onSave(
                            state.name,
                            state.teacher,
                            state.colorArgb,
                            state.dayOfWeek,
                            state.startSection,
                            state.endSection,
                            state.weeks,
                            state.location,
                            state.note,
                        )
                    }
                },
                onDelete = { confirmDelete = true },
                onCancel = onDismiss,
            )

            if (weeksError) {
                Text(
                    text = stringResource(DsR.string.sheet_weeks_required),
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

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.sheet_delete_title)) },
            text = { Text(stringResource(R.string.sheet_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(DsR.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(DsR.string.action_cancel))
                }
            },
        )
    }
}
