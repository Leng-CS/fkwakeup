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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS course_reminder_rules (
                course_id INTEGER NOT NULL PRIMARY KEY,
                recurring_mode TEXT NOT NULL,
                primary_minutes_before INTEGER NOT NULL,
                secondary_minutes_before INTEGER,
                async_open_enabled INTEGER NOT NULL,
                async_open_minutes_of_day INTEGER NOT NULL,
                async_deadline_enabled INTEGER NOT NULL,
                async_deadline_days_before INTEGER NOT NULL,
                async_deadline_minutes_of_day INTEGER NOT NULL,
                FOREIGN KEY(course_id) REFERENCES courses(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS reminder_occurrence_overrides (
                occurrence_key TEXT NOT NULL PRIMARY KEY,
                course_id INTEGER NOT NULL,
                kind TEXT NOT NULL,
                source_id INTEGER NOT NULL,
                week_number INTEGER,
                enabled INTEGER NOT NULL,
                primary_minutes_before INTEGER,
                secondary_minutes_before INTEGER,
                FOREIGN KEY(course_id) REFERENCES courses(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminder_occurrence_overrides_course_id ON reminder_occurrence_overrides(course_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminder_occurrence_overrides_source_id ON reminder_occurrence_overrides(source_id)")
    }
}
