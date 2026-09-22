package com.lengcs.fkwakeup.widget.glance

import com.lengcs.fkwakeup.core.database.repository.CourseRepository
import com.lengcs.fkwakeup.core.database.repository.TermRepository
import com.lengcs.fkwakeup.core.datastore.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDependencies {
    fun termRepository(): TermRepository
    fun courseRepository(): CourseRepository
    fun settingsRepository(): SettingsRepository
    fun widgetSettingsRepository(): WidgetSettingsRepository
}
