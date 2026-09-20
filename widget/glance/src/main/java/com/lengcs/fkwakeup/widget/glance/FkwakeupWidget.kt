package com.lengcs.fkwakeup.widget.glance

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lengcs.fkwakeup.widget.glance.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 「最近课程」小组件（#30）。
 *
 * 内容只有一种逻辑 —— 今天尚未结束的课程，按开始时间排序：
 * - **紧凑版**（[FkwakeupCompactWidget]，默认 **4 宽 × 2 高的横条**）：
 *   最近 2 节**并排**两列，无标题行
 * - **详细版**（[FkwakeupWidget]，默认 4×4）：最近 4 节竖排 + 「日期 · 第 N 周」标题行
 *
 * 条数上限由入口决定，拉伸只改行高/列宽不增减条数；空间不足时显示放得下的条数。
 * 点击任意位置打开 App 周视图。
 */
abstract class NextLessonsWidget(
    private val maxLessons: Int,
    private val showHeader: Boolean,
    private val horizontal: Boolean,
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetLoader.load(context)
        provideContent {
            NextLessonsContent(
                data = data,
                maxLessons = maxLessons,
                showHeader = showHeader,
                horizontal = horizontal,
                context = context,
            )
        }
    }
}

/** 详细版：默认 4×4，最近 4 节竖排 + 标题行 */
class FkwakeupWidget : NextLessonsWidget(maxLessons = 4, showHeader = true, horizontal = false) {

    companion object {
        /** 刷新本类的全部实例（供 [WidgetRefreshScheduler] 调用） */
        suspend fun refreshAll(context: Context) {
            withContext(Dispatchers.Default) {
                val manager = GlanceAppWidgetManager(context)
                manager.getGlanceIds(FkwakeupWidget::class.java)
                    .forEach { glanceId -> FkwakeupWidget().update(context, glanceId) }
            }
        }
    }
}

/** 紧凑版：默认 4 宽 × 2 高横条，最近 2 节并排，无标题行 */
class FkwakeupCompactWidget :
    NextLessonsWidget(maxLessons = 2, showHeader = false, horizontal = true) {

    companion object {
        suspend fun refreshAll(context: Context) {
            withContext(Dispatchers.Default) {
                val manager = GlanceAppWidgetManager(context)
                manager.getGlanceIds(FkwakeupCompactWidget::class.java)
                    .forEach { glanceId -> FkwakeupCompactWidget().update(context, glanceId) }
            }
        }
    }
}

/** 刷新两个入口的全部实例 */
internal suspend fun refreshAllWidgets(context: Context) {
    FkwakeupWidget.refreshAll(context)
    FkwakeupCompactWidget.refreshAll(context)
}

@Composable
internal fun NextLessonsContent(
    data: WidgetData?,
    maxLessons: Int,
    showHeader: Boolean,
    horizontal: Boolean,
    context: Context,
) {
    val launchComponent: ComponentName? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.component

    // 点击行为挂在**内层**内容区，不能挂在根上 ——
    // 根级的 GlanceModifier.clickable 会生成覆盖整个 RemoteViews 的 PendingIntent，
    // 把 launcher 的长按（移除 / 调整大小）也一起吃掉，用户将无法管理小组件。
    val contentModifier = if (launchComponent != null) {
        GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(launchComponent))
    } else {
        GlanceModifier.fillMaxSize()
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.surface)
            .padding(10.dp),
    ) {
        Box(modifier = contentModifier) {
            when {
                data == null -> CenteredText(stringRes = R.string.widget_no_term)
                data.lessons.isEmpty() -> CenteredText(stringRes = R.string.widget_no_more)
                else -> LessonList(data, maxLessons, showHeader, horizontal)
            }
        }
    }
}

@Composable
private fun CenteredText(stringRes: Int) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = LocalString(stringRes),
            style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 13.sp),
        )
    }
}

/**
 * 课程条目排布：
 * - **竖排**（详细版）：标题行 + 每条一行，平分剩余高度
 * - **横排**（紧凑版，4 宽 × 2 高的横条）：两节课**并排**两列，各占一半宽度
 */
@Composable
private fun LessonList(
    data: WidgetData,
    maxLessons: Int,
    showHeader: Boolean,
    horizontal: Boolean,
) {
    if (horizontal) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            data.lessons.take(maxLessons).forEachIndexed { index, lesson ->
                if (index > 0) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                }
                LessonRow(
                    lesson = lesson,
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                )
            }
        }
    } else {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            if (showHeader) {
                Text(
                    text = "${data.dateText} · ${data.weekText}",
                    style = TextStyle(
                        color = WidgetColors.onSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
            data.lessons.take(maxLessons).forEach { lesson ->
                LessonRow(lesson = lesson, modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

/** 一条课程：左侧色条 + 课名（+「正在上」）+ 时间节次 + 地点 */
@Composable
private fun LessonRow(lesson: WidgetLesson, modifier: GlanceModifier) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .then(modifier)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧课程色竖条 —— 与周视图/颜色粒度保持一致（同名课同色）
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Color(lesson.colorArgb)),
        ) {}
        Spacer(modifier = GlanceModifier.width(7.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lesson.name,
                    style = TextStyle(
                        color = WidgetColors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                if (lesson.isOngoing) {
                    Spacer(modifier = GlanceModifier.width(5.dp))
                    Text(
                        text = LocalString(R.string.widget_ongoing),
                        style = TextStyle(
                            color = WidgetColors.ongoingAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = "${lesson.timeText} · ${lesson.sectionText}",
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 10.sp),
                maxLines = 1,
            )
            Text(
                text = "@${lesson.location ?: LocalString(R.string.widget_location_unknown)}",
                style = TextStyle(color = WidgetColors.onSurfaceVariant, fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LocalString(resId: Int): String = LocalContext.current.getString(resId)

/** 小组件配色（日/夜各一份，走资源限定符，见 WidgetColors 注释历史） */
private object WidgetColors {
    val surface = ColorProvider(R.color.widget_surface)
    val onSurface = ColorProvider(R.color.widget_on_surface)
    val onSurfaceVariant = ColorProvider(R.color.widget_on_surface_variant)
    val ongoingAccent = ColorProvider(R.color.widget_ongoing_accent)
}
