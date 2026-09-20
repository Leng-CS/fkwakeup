package com.lengcs.fkwakeup.widget.glance

import android.content.Context
import dagger.hilt.android.EntryPointAccessors

/** 小组件数据加载的统一入口（两个形态共用同一份数据，条数各自裁剪） */
internal object WidgetLoader {

    suspend fun load(context: Context): WidgetData? {
        val deps = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDependencies::class.java,
        )
        return WidgetDataProvider(
            termRepository = deps.termRepository(),
            courseRepository = deps.courseRepository(),
            settingsRepository = deps.settingsRepository(),
        ).load()
    }
}
