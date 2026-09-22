package com.lengcs.fkwakeup.widget.glance

import android.content.Context
import dagger.hilt.android.EntryPointAccessors

/** 供渲染和精确刷新调度共用的数据加载入口。 */
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
