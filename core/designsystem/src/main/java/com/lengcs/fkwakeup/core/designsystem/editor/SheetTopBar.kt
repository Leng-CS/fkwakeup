package com.lengcs.fkwakeup.core.designsystem.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lengcs.fkwakeup.core.designsystem.R

/**
 * 编辑抽屉的顶部操作条：左侧标题，右侧图标按钮。
 *
 * [onDelete] 为 null 时不显示删除按钮 —— 导入预览页不需要「删除本节课」
 * （删除入口在课程卡片上），周视图抽屉需要。
 *
 * **布局注意**：这里用 `Box` + `align(CenterStart/CenterEnd)`，而不是
 * `Row` + `Text(Modifier.weight(1f))`。实测后者的 weight 不会把剩余宽度让出来，
 * `Row` 被压成标题文字宽度，`IconButton` 变 0 宽 —— UI 树里能查到节点却完全不可见。
 */
@Composable
fun SheetTopBar(
    title: String,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null,
    saveEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = onSave, enabled = saveEnabled) {
                Icon(
                    painter = painterResource(R.drawable.ic_save),
                    contentDescription = stringResource(R.string.sheet_save),
                    tint = if (saveEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.sheet_delete_session),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = onCancel) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.sheet_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
