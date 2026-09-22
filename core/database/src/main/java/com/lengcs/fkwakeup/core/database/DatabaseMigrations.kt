package com.lengcs.fkwakeup.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE course_sessions ADD COLUMN delivery_mode TEXT NOT NULL DEFAULT 'ONSITE'")
        db.execSQL("ALTER TABLE course_sessions ADD COLUMN online_platform TEXT")
        db.execSQL("ALTER TABLE course_sessions ADD COLUMN online_url TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS online_course_windows (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                course_id INTEGER NOT NULL,
                start_date_epoch_day INTEGER NOT NULL,
                end_date_epoch_day INTEGER NOT NULL,
                platform TEXT,
                url TEXT,
                note TEXT,
                FOREIGN KEY(course_id) REFERENCES courses(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_online_course_windows_course_id ON online_course_windows(course_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_online_course_windows_start_date_epoch_day ON online_course_windows(start_date_epoch_day)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_online_course_windows_end_date_epoch_day ON online_course_windows(end_date_epoch_day)")
    }
}
