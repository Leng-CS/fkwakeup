package com.lengcs.fkwakeup.widget.glance

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lengcs.fkwakeup.widget.glance.R
import com.lengcs.fkwakeup.core.common.BlockPhase
import com.lengcs.fkwakeup.core.common.CourseTextColor
import com.lengcs.fkwakeup.core.common.MutedBlockColor
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * 桌面小组件。三种尺寸：
 * - 小 2×2：下一节课
 * - 中 4×2：今日剩余（最多 3 条）
 * - 大 4×4：本周网格 + 今日列高亮
 *
 * 硬约束（主开发文档 7.3）：
 * - 只能用 Box / Row / Column / Text / Image / Icon / Button / Spacer / LazyColumn
 * - 不能用 Canvas、自定义 Layout、Compose 动画、自定义字体
 * - 尺寸按 [LocalSize] 动态分支，禁止假设固定尺寸
 * - 颜色走 [ColorProvider]（日/夜各一份），不能用裸 Color
 */
class FkwakeupWidget : GlanceAppWidget() {

    /** Exact：把真实尺寸交给 LocalSize，由 UI 自己分支 */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDependencies::class.java,
        )
        val provider = WidgetDataProvider(
            termRepository = deps.termRepository(),
            courseRepository = deps.courseRepository(),
            settingsRepository = deps.settingsRepository(),
        )
        val data = provider.load()

        provideContent {
            WidgetContent(data = data, context = context)
        }
    }

    companion object {
        /** 写库后主动刷新，保证「改完课立刻更新」 */
        suspend fun refresh(context: Context) {
            withContext(Dispatchers.Default) {
                val widget = FkwakeupWidget()
                val manager = GlanceAppWidgetManager(context)
                manager.getGlanceIds(FkwakeupWidget::class.java).forEach { glanceId ->
                    widget.update(context, glanceId)
                }
            }
        }
    }
}

@Composable
private fun WidgetContent(
    data: WidgetData?,
    context: Context,
) {
    val size = LocalSize.current
    val launchComponent: ComponentName? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.component

    val baseModifier = GlanceModifier
        .fillMaxSize()
        .background(WidgetColors.surface)
        .padding(12.dp)
    val modifier = if (launchComponent != null) {
        baseModifier.clickable(actionStartActivity(launchComponent))
    } else {
        baseModifier
    }

    Box(modifier = modifier) {
        when {
            data == null -> NoTermView()
            size.height >= 240.dp -> LargeView(data, size.width)
            size.width >= 200.dp -> MediumView(data, size.width)
            else -> SmallView(data)
        }
    }
}

// ---- 小尺寸：下一节课 ----

@Composable
private fun SmallView(data: WidgetData) {
    val next = data.nextLesson
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = data.weekText,
            style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        if (next == null) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "今天没有更多课",
                    style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 13.sp),
                )
            }
        } else {
            Text(
                text = next.name,
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = next.timeText,
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 12.sp),
            )
            if (next.location != null) {
                Text(
                    text = "@${next.location}",
                    style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

// ---- 中尺寸：今日剩余 ----

@Composable
private fun MediumView(data: WidgetData, totalWidth: Dp) {
    val nameWidth = totalWidth - 88.dp
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = "今日剩余 · ${data.weekText}",
            style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (data.remainingToday.isEmpty()) {
            Text(
                text = "今天没有更多课",
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 13.sp),
            )
        } else {
            data.remainingToday.take(3).forEach { lesson ->
                Row(modifier = GlanceModifier.fillMaxWidth().padding(2.dp)) {
                    Text(
                        text = lesson.timeText,
                        style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 11.sp),
                        modifier = GlanceModifier.width(84.dp),
                    )
                    Column(modifier = GlanceModifier.width(nameWidth)) {
                        Text(
                            text = lesson.name,
                            style = TextStyle(color = WidgetColors.onSurface, fontSize = 13.sp),
                            maxLines = 1,
                        )
                        if (lesson.location != null) {
                            Text(
                                text = "@${lesson.location}",
                                style = TextStyle(
                                    color = WidgetColors.onSurfaceVariant,
                                    fontSize = 10.sp,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---- 大尺寸：本周网格 ----

@Composable
private fun LargeView(data: WidgetData, totalWidth: Dp) {
    val size = LocalSize.current
    val rows = data.weekGrid.size.coerceAtLeast(1)
    val headerHeight = 20.dp
    val cellWidth = totalWidth / 7
    val cellHeight = (size.height - headerHeight - 20.dp) / rows
    val todayIndex = LocalDate.now().dayOfWeek.value - 1

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = "${data.termName ?: "课表"} · ${data.weekText}",
            style = TextStyle(
                color = WidgetColors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))

        Row(modifier = GlanceModifier.fillMaxWidth().height(headerHeight)) {
            WEEKDAYS.forEach { label ->
                Box(
                    modifier = GlanceModifier.width(cellWidth).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 10.sp),
                    )
                }
            }
        }

        // 网格：每格标课名。跨节次的课会重复出现在它覆盖的每一节 ——
        // Glance 没有自定义 Layout，做不了 App 里那种跨行长块
        data.weekGrid.forEach { row ->
            Row(modifier = GlanceModifier.fillMaxWidth().height(cellHeight)) {
                row.forEachIndexed { dayIndex, cell ->
                    val isToday = dayIndex == todayIndex
                    // #29：已上完的课变灰，与 App 内周视图保持一致。
                    // 文字颜色要按**灰化后**的颜色算 —— 灰化会改变亮度，
                    // 沿用原色判断可能让浅灰底配白字看不清。
                    val cellArgb = if (cell != null && cell.phase == BlockPhase.Past) {
                        MutedBlockColor.mute(cell.colorArgb)
                    } else {
                        cell?.colorArgb
                    }
                    Box(
                        modifier = GlanceModifier
                            .size(width = cellWidth, height = cellHeight)
                            .padding(1.dp)
                            .background(
                                when {
                                    // 有课就用课程自己的颜色（用户自定义色或按课名哈希）
                                    cellArgb != null -> ColorProvider(Color(cellArgb))
                                    isToday -> WidgetColors.todayColumn
                                    else -> WidgetColors.gridLine
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell != null && cellArgb != null) {
                            Text(
                                text = cell.label,
                                style = TextStyle(
                                    // 自定义色不一定是深色，按亮度切换黑/白字
                                    color = if (CourseTextColor.shouldUseDarkText(cellArgb)) {
                                        ColorProvider(Color.Black)
                                    } else {
                                        ColorProvider(Color.White)
                                    },
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoTermView() {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "还没有课表",
            style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 13.sp),
        )
    }
}

private val WEEKDAYS = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 小组件配色。
 *
 * 这个版本的 Glance 的 `ColorProvider` 只有 `ColorProvider(color)` 与
 * `ColorProvider(resId)` 两个工厂，**没有 day/night 重载**，
 * 所以深色模式走 Android 的资源限定符：同一组颜色名在
 * `values/colors.xml` 与 `values-night/colors.xml` 各定义一份，
 * 系统切深色时会自动取到夜间那份。
 */
private object WidgetColors {
    val surface = ColorProvider(R.color.widget_surface)
    val onSurface = ColorProvider(R.color.widget_on_surface)
    val onSurfaceVariant = ColorProvider(R.color.widget_on_surface_variant)
    val gridLine = ColorProvider(R.color.widget_grid_line)
    val todayColumn = ColorProvider(R.color.widget_today_column)
    val courseBlock = ColorProvider(R.color.widget_course_block)
    val onCourseBlock = ColorProvider(R.color.widget_on_course_block)
}
