package com.sysadmindoc.alarmclock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.model.Alarm

@Database(entities = [Alarm::class, AlarmEvent::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun alarmEventDao(): AlarmEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN challengeType TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS alarm_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        alarmId INTEGER NOT NULL,
                        alarmLabel TEXT NOT NULL DEFAULT '',
                        scheduledTime INTEGER NOT NULL,
                        firedAt INTEGER NOT NULL,
                        action TEXT NOT NULL,
                        actionAt INTEGER NOT NULL DEFAULT 0,
                        challengeType TEXT NOT NULL DEFAULT 'NONE',
                        challengeSolveTimeMs INTEGER NOT NULL DEFAULT 0,
                        snoozeCount INTEGER NOT NULL DEFAULT 0,
                        dayOfWeek INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
    }
}
