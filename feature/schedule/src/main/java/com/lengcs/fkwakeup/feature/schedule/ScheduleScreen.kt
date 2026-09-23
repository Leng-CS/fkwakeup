package com.lengcs.fkwakeup.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lengcs.fkwakeup.core.common.BlockPhase
import com.lengcs.fkwakeup.core.common.CourseTextColor
import com.lengcs.fkwakeup.core.common.MutedBlockColor
import com.lengcs.fkwakeup.core.common.ScheduleBlock
import com.lengcs.fkwakeup.core.designsystem.theme.FkwakeupTheme
import com.lengcs.fkwakeup.core.model.SectionTemplate
import com.lengcs.fkwakeup.core.model.SessionDeliveryMode

private val SECTION_COLUMN_WIDTH = 44.dp
/** 一格要放下「开始时间 + 课程名 + 地点」三行，56dp 太挤，放宽到 64dp */
private val ROW_HEIGHT = 64.dp
private val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@Composable
fun ScheduleScreen(
    onImportClick: () -> Unit,
    onManageClick: () -> Unit,
    onOnlineCourseClick: (Long) -> Unit,
    onOnlineAlarmClick: (Long) -> Unit,
    onReminderClick: (Long, String) -> Unit,
    targetDate: java.time.LocalDate? = null,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val conflicts by viewModel.conflicts.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingBlock by remember { mutableStateOf<ScheduleBlock?>(null) }

    LaunchedEffect(targetDate, state.term?.id) {
        targetDate?.let(viewModel::showDate)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScheduleTopBar(
            termName = state.term?.name,
            displayWeek = state.displayWeek,
            isCurrentWeek = state.displayWeek == state.currentWeek,
            onPrev = viewModel::previousWeek,
            onNext = viewModel::nextWeek,
            onToday = viewModel::backToCurrentWeek,
            onManage = onManageClick,
            // 空课表时顶栏不放导入按钮：那时页面中央已有主 CTA「导入课表」，
            // 顶栏再放一个是噪音（#25）
            onImport = if (state.term == null || state.isEmpty) null else onImportClick,
        )

        if (state.term == null || state.isEmpty) {
            EmptySchedule(onImportClick = onImportClick)
            return@Column
        }

        if (state.onlineCourses.isNotEmpty()) {
            OnlineCourseSection(
                items = state.onlineCourses,
                onCourseClick = onOnlineCourseClick,
                onAlarmClick = onOnlineAlarmClick,
            )
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
                    blockPhases = state.blockPhases,
                    onBlockClick = { editingBlock = it },
                )
            }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    editingBlock?.let { block ->
        CourseBlockEditSheet(
            block = block,
            totalWeeks = state.term?.totalWeeks ?: 18,
            sectionCount = state.sections.size.coerceAtLeast(1),
            onDismiss = { editingBlock = null },
            onSave = { name, teacher, color, dow, from, to, weeks, place, note ->
                viewModel.saveBlockEdits(
                    block = block,
                    name = name,
                    teacher = teacher,
                    colorArgb = color,
                    dayOfWeek = dow,
                    startSection = from,
                    endSection = to,
                    weeks = weeks,
                    location = place,
                    note = note,
                    onDone = { editingBlock = null },
                )
            },
            onDelete = {
                viewModel.deleteBlock(block) { editingBlock = null }
            },
            onReminder = {
                onReminderClick(
                    block.course.id,
                    com.lengcs.fkwakeup.core.model.ReminderOccurrenceOverride.sessionKey(block.session.id, state.displayWeek),
                )
            },
        )
    }
    if (conflicts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConflict,
            title = { Text("发现课程时间冲突") },
            text = { Text(conflicts.take(3).joinToString("\n") { it.message() }) },
            confirmButton = { TextButton(onClick = viewModel::confirmConflict) { Text("仍然保存") } },
            dismissButton = { TextButton(onClick = viewModel::dismissConflict) { Text("返回修改") } },
        )
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
    /** null = 不显示导入入口（空课表时） */
    onImport: (() -> Unit)?,
) {
    val prevDesc = stringResource(R.string.schedule_prev_cd)
    val nextDesc = stringResource(R.string.schedule_next_cd)
    val todayDesc = stringResource(R.string.schedule_today_cd)

    val weekText = buildString {
        append(stringResource(R.string.schedule_week_label, displayWeek))
        if (isCurrentWeek) append(" · ").append(stringResource(R.string.schedule_week_current))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = termName ?: stringResource(R.string.schedule_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = weekText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    )
                }
                NavButton(text = "‹", desc = prevDesc, onClick = onPrev)
                if (!isCurrentWeek) {
                    NavButton(text = "•", desc = todayDesc, onClick = onToday)
                }
                NavButton(text = "›", desc = nextDesc, onClick = onNext)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 两个同级入口共用胶囊样式，形成清楚的操作组。
                onImport?.let { importClick ->
                    HeaderActionButton(
                        text = stringResource(R.string.schedule_import),
                        onClick = importClick,
                    )
                }

                HeaderActionButton(
                    text = stringResource(R.string.schedule_manage),
                    onClick = onManage,
                )
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    text: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(34.dp),
        shape = CircleShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun NavButton(
    text: String,
    desc: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(start = 4.dp)
            .size(34.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), CircleShape)
            .semantics { contentDescription = desc },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WeekdayHeader(todayDayOfWeek: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.width(SECTION_COLUMN_WIDTH))
        WEEKDAYS.forEachIndexed { index, label ->
            val isToday = todayDayOfWeek == index + 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp, vertical = 4.dp)
                    .background(
                        color = if (isToday) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            Color.Transparent
                        },
                        shape = MaterialTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onTertiaryContainer
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
    Column(
        modifier = Modifier
            .width(SECTION_COLUMN_WIDTH)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        sections.forEach { section ->
            val isCurrent = currentSection == section.index
            Box(
                modifier = Modifier
                    .height(ROW_HEIGHT)
                    .fillMaxWidth()
                    .padding(3.dp)
                    .background(
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                        shape = MaterialTheme.shapes.small,
                    ),
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
    /** 每个课块相对此刻的状态（#29）：已上完的变灰、正在上的加边框 */
    blockPhases: Map<Long, BlockPhase>,
    onBlockClick: (ScheduleBlock) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnWidth: Dp = maxWidth / 7
        val gridHeight: Dp = ROW_HEIGHT * sectionCount.coerceAtLeast(1)

        Box(
            modifier = Modifier
                .height(gridHeight)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
        ) {
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
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
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
                // 是否已上完由 BlockPhaseCalculator 按「日期 + 起止时间」判定（#29）。
                // 早先这里只判定「今天 + 节次比当前靠前」，于是周一到周五哪怕早就过完了仍是全彩。
                val phase = blockPhases[block.session.id] ?: BlockPhase.Upcoming
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
                        .padding(1.dp)
                        .clickable { onBlockClick(block) },
                ) {
                    CourseBlockCard(block = block, phase = phase)
                }
            }
        }
    }
}

@Composable
private fun CourseBlockCard(
    block: ScheduleBlock,
    phase: BlockPhase,
) {
    // 颜色在布局阶段已解析好（自定义色优先，否则按课名哈希），这里直接用。
    // #29：已上完的课去饱和变灰（而不是像早先那样只降不透明度 —— 那还留着颜色，
    // 看着是「淡」不是「灰」）。正在上的保持全彩并加一圈边框突出。
    val blockArgb = if (phase == BlockPhase.Past) {
        MutedBlockColor.mute(block.colorArgb)
    } else {
        block.colorArgb
    }
    val baseColor = Color(blockArgb)
    // 自定义色不一定是深色，按亮度决定用黑字还是白字，避免浅色块上白字看不清。
    // 注意要按**实际显示的颜色**算，灰化后亮度可能跨过阈值。
    val onBlockColor = if (CourseTextColor.shouldUseDarkText(blockArgb)) {
        Color.Black
    } else {
        Color.White
    }

    val cardShape = RoundedCornerShape(10.dp)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = if (phase == BlockPhase.Ongoing) 2.dp else 1.dp,
                color = if (phase == BlockPhase.Ongoing) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White.copy(alpha = 0.58f)
                },
                shape = cardShape,
            ),
        shape = cardShape,
        color = baseColor,
        contentColor = onBlockColor,
    ) {
        Column(modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)) {
            // 1. 开始时间
            Text(
                text = block.startMinutes?.let { formatMinutes(it) }
                    ?: "第${block.session.startSection}节",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                maxLines = 1,
                color = onBlockColor.copy(alpha = 0.85f),
            )
            // 2. 课程名：给足行数，尽量不截断
            Text(
                text = block.course.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 13.sp,
                ),
                maxLines = 3,
                overflow = TextOverflow.Clip,
            )
            // 3. 上课地点或直播平台
            val place = if (block.session.deliveryMode == SessionDeliveryMode.LIVE_ONLINE) {
                block.session.onlinePlatform?.let { "直播 · $it" } ?: "直播网课"
            } else {
                block.session.location?.let { "@$it" }
            }
            if (place != null) {
                Text(
                    text = place,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    color = onBlockColor.copy(alpha = 0.85f),
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
