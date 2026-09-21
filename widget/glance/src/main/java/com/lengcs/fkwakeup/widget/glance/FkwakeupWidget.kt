package com.lengcs.fkwakeup.widget.glance

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
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
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 单入口、可纵向滚动、按桌面实例配置的课程列表小组件。 */
class FkwakeupWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDependencies::class.java,
        )
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val config = deps.widgetSettingsRepository().config(appWidgetId)
        val data = WidgetDataProvider(
            termRepository = deps.termRepository(),
            courseRepository = deps.courseRepository(),
            settingsRepository = deps.settingsRepository(),
        ).load(range = config.range)
        provideContent { WidgetContent(data, config, context) }
    }

    companion object {
        suspend fun refresh(context: Context) {
            withContext(Dispatchers.Default) {
                val widget = FkwakeupWidget()
                val manager = GlanceAppWidgetManager(context)
                manager.getGlanceIds(FkwakeupWidget::class.java).forEach { id ->
                    widget.update(context, id)
                }
            }
        }
    }
}

@Composable
private fun WidgetContent(data: WidgetData?, config: WidgetConfig, context: Context) {
    // 根节点故意不响应点击：否则会吞掉桌面长按的移除/调整大小菜单。
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .then(config.backgroundModifier())
            .padding(10.dp),
    ) {
        when {
            data == null -> EmptyContent("还没有课表", context.openAction())
            data.lessons.isEmpty() -> EmptyContent(
                data.nextLesson?.let { "下一次：${it.dateText} · ${it.name}" } ?: "没有更多课程",
                context.openAction(data.nextLesson?.let { "schedule?date=${it.date}" }),
            )
            else -> LessonList(data, config, context)
        }
    }
}

@Composable
private fun EmptyContent(text: String, action: Action?) {
    Box(
        modifier = action?.let { GlanceModifier.fillMaxSize().clickable(it) } ?: GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = TextStyle(color = WidgetConfig().secondaryTextColor(), fontSize = 13.sp), maxLines = 2)
    }
}

@Composable
private fun LessonList(data: WidgetData, config: WidgetConfig, context: Context) {
    val size = LocalSize.current
    val rowHeight = if (size.height < 112.dp) 52.dp else 56.dp
    LazyColumn(modifier = GlanceModifier.fillMaxSize().then(context.openAction()?.let(GlanceModifier::clickable) ?: GlanceModifier)) {
        item(itemId = 0L) {
            Text(
                text = "${data.termName} · ${data.weekText}",
                style = TextStyle(color = config.secondaryTextColor(), fontSize = 11.sp),
                maxLines = 1,
                modifier = context.openAction()?.let { GlanceModifier.fillMaxWidth().clickable(it) }
                    ?: GlanceModifier.fillMaxWidth(),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
        items(data.lessons, itemId = { lesson -> lesson.stableId.hashCode().toLong() }) { lesson ->
            lesson.dateHeader?.let { header ->
                Text(
                    text = header,
                    style = TextStyle(color = config.secondaryTextColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(top = 6.dp, bottom = 2.dp),
                )
            }
            LessonRow(lesson, config, rowHeight, context)
        }
    }
}

@Composable
private fun LessonRow(lesson: WidgetLesson, config: WidgetConfig, rowHeight: androidx.compose.ui.unit.Dp, context: Context) {
    val courseAction = context.openAction("schedule?date=${lesson.date}")
    androidx.glance.layout.Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(config.cardColor())
            .padding(vertical = 3.dp)
            .then(courseAction?.let { GlanceModifier.clickable(it) } ?: GlanceModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LessonBar(lesson, config)
        Spacer(modifier = GlanceModifier.width(7.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "${lesson.startSection} · ${lesson.startTime}  ${lesson.name}",
                style = TextStyle(color = config.primaryTextColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                text = "${lesson.endSection} · ${lesson.endTime}  ${lesson.location ?: "地点未知"} · ${lesson.teacher ?: "教师未知"}",
                style = TextStyle(color = config.secondaryTextColor(), fontSize = 10.sp),
                maxLines = 1,
            )
        }
        Text(
            text = "⏰",
            style = TextStyle(color = config.secondaryTextColor(), fontSize = 16.sp),
            modifier = context.openAction("alarm")?.let { GlanceModifier.padding(horizontal = 4.dp).clickable(it) }
                ?: GlanceModifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun LessonBar(lesson: WidgetLesson, config: WidgetConfig) {
    when (config.barMode) {
        com.lengcs.fkwakeup.core.common.WidgetLessonBarMode.COURSE_COLOR -> ColorBar(lesson.colorArgb)
        com.lengcs.fkwakeup.core.common.WidgetLessonBarMode.SECTION -> SectionBar(lesson, config)
        com.lengcs.fkwakeup.core.common.WidgetLessonBarMode.COLOR_AND_SECTION -> {
            ColorBar(lesson.colorArgb)
            Spacer(modifier = GlanceModifier.width(3.dp))
            SectionBar(lesson, config)
        }
    }
}

@Composable
private fun ColorBar(colorArgb: Int) {
    Box(
        modifier = GlanceModifier.width(4.dp).fillMaxHeight().background(Color(colorArgb)),
    ) {}
}

@Composable
private fun SectionBar(lesson: WidgetLesson, config: WidgetConfig) {
    Box(modifier = GlanceModifier.width(27.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "${lesson.startSection}–${lesson.endSection}",
            style = TextStyle(color = config.secondaryTextColor(), fontSize = 9.sp),
            maxLines = 1,
        )
    }
}

private fun Context.openAction(path: String? = null): Action? =
    packageManager.getLaunchIntentForPackage(packageName)?.apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("fkwakeup://widget/${path.orEmpty()}")
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }?.let(::actionStartActivity)
