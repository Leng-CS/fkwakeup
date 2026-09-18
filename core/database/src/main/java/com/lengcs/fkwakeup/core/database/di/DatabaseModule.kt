package com.lengcs.fkwakeup.core.database.di

import android.content.Context
import androidx.room.Room
import com.lengcs.fkwakeup.core.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            // 小组件进程与主进程可能并发读写，必须开启多实例失效
            .enableMultiInstanceInvalidation()
            .build()

    @Provides
    fun provideTermDao(db: AppDatabase) = db.termDao()

    @Provides
    fun provideSectionTemplateDao(db: AppDatabase) = db.sectionTemplateDao()

    @Provides
    fun provideCourseDao(db: AppDatabase) = db.courseDao()

    @Provides
    fun provideCourseSessionDao(db: AppDatabase) = db.courseSessionDao()
}
