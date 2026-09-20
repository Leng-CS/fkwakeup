package com.lengcs.fkwakeup.widget.glance

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** 详细版小组件（默认 4×4，最近 4 节 + 标题行） */
class FkwakeupWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FkwakeupWidget()
}

/** 紧凑版小组件（默认 2×4，最近 2 节） */
class FkwakeupCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FkwakeupCompactWidget()
}
