package com.lengcs.fkwakeup.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lengcs.fkwakeup.core.common.ScheduleBlock
import com.lengcs.fkwakeup.core.designsystem.palette.CoursePalette
import com.lengcs.fkwakeup.core.designsystem.theme.FkwakeupTheme
import com.lengcs.fkwakeup.core.model.SectionTemplate

private val SECTION_COLUMN_WIDTH = 44.dp
private val ROW_HEIGHT = 56.dp
private val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@Composable
fun ScheduleScreen(
    onImportClick: () -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        ScheduleTopBar(
            termName = state.term?.name,
            displayWeek = state.displayWeek,
            isCurrentWeek = state.displayWeek == state.currentWeek,
            onPrev = viewModel::previousWeek,
            onNext = viewModel::nextWeek,
            onToday = viewModel::backToCurrentWeek,
            onManage = onManageClick,
        )

        if (state.term == null || state.isEmpty) {
            EmptySchedule(onImportClick = onImportClick)
            return@Column
        }

        WeekdayHeader(todayDayOfWeek = state.todayDayOfWeek)

        Row(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionColumn(sections = state.sections, currentSection = state.currentSection)
            // weight 必须在 Row 作用域里用，不能直接挂在 BoxWithConstraints 上
            Box(modifier = Modifier.weight(1f)) {
                ScheduleGrid(
                    blocks = state.blocks,
                    sectionCount = state.sections.size,
                    todayDayOfWeek = state.todayDayOfWeek,
                    currentSection = state.currentSection,
                )
            }
        }
    }
}

@Composable
private fun ScheduleTopBar(
    termName: String?,
    displayWeek: Int,
    isCurrentWeek: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onManage: () -> Unit,
) {
    val prevDesc = stringResource(R.string.schedule_prev_cd)
    val nextDesc = stringResource(R.string.schedule_next_cd)
    val todayDesc = stringResource(R.string.schedule_today_cd)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = termName ?: stringResource(R.string.schedule_title),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        TextButton(onClick = onManage) { Text("管理", style = MaterialTheme.typography.labelMedium) }
        Text(
            text = buildString {
                append(stringResource(R.string.schedule_week_label, displayWeek))
                if (isCurrentWeek) append(" · ").append(stringResource(R.string.schedule_week_current))
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        // 图标只有符号，必须给读屏软件补语义描述
        IconButton(
            onClick = onPrev,
            modifier = Modifier.semantics { contentDescription = prevDesc },
        ) { Text("<", textAlign = TextAlign.Center) }
        IconButton(
            onClick = onToday,
            modifier = Modifier.semantics { contentDescription = todayDesc },
        ) { Text("·") }
        IconButton(
            onClick = onNext,
            modifier = Modifier.semantics { contentDescription = nextDesc },
        ) { Text(">") }
    }
}

@Composable
private fun WeekdayHeader(todayDayOfWeek: Int?) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp)) {
        Box(modifier = Modifier.width(SECTION_COLUMN_WIDTH))
        WEEKDAYS.forEachIndexed { index, label ->
            val isToday = todayDayOfWeek == index + 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isToday) {
                            Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionColumn(
    sections: List<SectionTemplate>,
    currentSection: Int?,
) {
    Column(modifier = Modifier.width(SECTION_COLUMN_WIDTH)) {
        sections.forEach { section ->
            val isCurrent = currentSection == section.index
            Box(
                modifier = Modifier
                    .height(ROW_HEIGHT)
                    .fillMaxWidth()
                    .then(
                        if (isCurrent) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            Modifier
                        },
                    )
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = section.index.toString(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = formatMinutes(section.startMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * 网格 + 课程块。
 *
 * 用绝对定位的 Box 叠加层而不是格子布局 —— 这一个方案同时解决跨节次、
 * 重叠冲突、对齐三个问题（主开发文档 7.1）。
 */
@Composable
private fun ScheduleGrid(
    blocks: List<ScheduleBlock>,
    sectionCount: Int,
    todayDayOfWeek: Int?,
    currentSection: Int?,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnWidth: Dp = maxWidth / 7
        val gridHeight: Dp = ROW_HEIGHT * sectionCount.coerceAtLeast(1)

        Box(modifier = Modifier.height(gridHeight).fillMaxWidth()) {
            // 底层：7 列网格线
            Row(modifier = Modifier.matchParentSize()) {
                repeat(7) { index ->
                    val isToday = todayDayOfWeek == index + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (isToday) {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }

            // 上层：课程块，绝对定位
            blocks.forEach { block ->
                // 只有「今天」且已经上完的课才降透明度 ——
                // 别的天的课不能因为节次比当前靠前就判定为已结束
                val isToday = todayDayOfWeek != null && block.dayIndex == todayDayOfWeek - 1
                val ended = isToday &&
                    currentSection != null &&
                    (block.startRow + block.rowSpan - 1) < (currentSection - 1)
                Box(
                    modifier = Modifier
                        .offset(
                            x = columnWidth * (block.dayIndex + block.columnIndex.toFloat() / block.columnCount),
                            y = ROW_HEIGHT * block.startRow,
                        )
                        .size(
                            width = columnWidth / block.columnCount,
                            height = ROW_HEIGHT * block.rowSpan,
                        )
                        .padding(1.dp),
                ) {
                    CourseBlockCard(block = block, ended = ended)
                }
            }
        }
    }
}

@Composable
private fun CourseBlockCard(
    block: ScheduleBlock,
    ended: Boolean,
) {
    val baseColor = block.course.colorArgb?.let { Color(it) }
        ?: CoursePalette.pickFor(block.course.name)
    val alpha = if (ended) 0.5f else 1f

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.small,
        color = baseColor.copy(alpha = baseColor.alpha * alpha),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = block.course.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            block.session.location?.let { location ->
                Text(
                    text = "@$location",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptySchedule(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.schedule_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(
            onClick = onImportClick,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(0.6f),
        ) {
            Text(stringResource(R.string.schedule_empty_action))
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}
