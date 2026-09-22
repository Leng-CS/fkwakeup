package com.lengcs.fkwakeup.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lengcs.fkwakeup.core.database.dao.CourseDao
import com.lengcs.fkwakeup.core.database.dao.CourseSessionDao
import com.lengcs.fkwakeup.core.database.dao.OnlineCourseWindowDao
import com.lengcs.fkwakeup.core.database.dao.SectionTemplateDao
import com.lengcs.fkwakeup.core.database.dao.TermDao
import com.lengcs.fkwakeup.core.database.entity.CourseEntity
import com.lengcs.fkwakeup.core.database.entity.CourseSessionEntity
import com.lengcs.fkwakeup.core.database.entity.OnlineCourseWindowEntity
import com.lengcs.fkwakeup.core.database.entity.SectionTemplateEntity
import com.lengcs.fkwakeup.core.database.entity.TermEntity

@Database(
    entities = [
        TermEntity::class,
        SectionTemplateEntity::class,
        CourseEntity::class,
        CourseSessionEntity::class,
        OnlineCourseWindowEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun termDao(): TermDao
    abstract fun sectionTemplateDao(): SectionTemplateDao
    abstract fun courseDao(): CourseDao
    abstract fun courseSessionDao(): CourseSessionDao
    abstract fun onlineCourseWindowDao(): OnlineCourseWindowDao

    companion object {
        const val DB_NAME = "fkwakeup.db"
    }
}
