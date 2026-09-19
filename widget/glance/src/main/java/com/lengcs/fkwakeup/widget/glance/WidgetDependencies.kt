package com.lengcs.fkwakeup.widget.glance

import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 小组件的依赖入口。
 *
 * GlanceAppWidget 不是 Android 组件，没法直接 @Inject 字段，
 * 所以用 Hilt EntryPoint 从 Application 里取。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDependencies {
    fun termRepository(): TermRepository
    fun courseRepository(): CourseRepository
    fun settingsRepository(): SettingsRepository
}
