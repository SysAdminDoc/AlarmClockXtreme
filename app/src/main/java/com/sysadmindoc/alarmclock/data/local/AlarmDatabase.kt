package com.sysadmindoc.alarmclock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.model.Alarm

@Database(entities = [Alarm::class, AlarmEvent::class], version = 8, exportSchema = true)
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN `group` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alarms ADD COLUMN flashWake INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN vibrationPattern TEXT NOT NULL DEFAULT 'default'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // F11: TTS morning announcement
                db.execSQL("ALTER TABLE alarms ADD COLUMN ttsEnabled INTEGER NOT NULL DEFAULT 0")
                // F4: Walk-steps challenge
                db.execSQL("ALTER TABLE alarms ADD COLUMN walkStepsRequired INTEGER NOT NULL DEFAULT 30")
                // F5: Post-alarm wake confirmation
                db.execSQL("ALTER TABLE alarms ADD COLUMN wakeConfirmEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN wakeConfirmDelayMinutes INTEGER NOT NULL DEFAULT 10")
                // F7: Smart alarm window
                db.execSQL("ALTER TABLE alarms ADD COLUMN smartAlarmEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN smartAlarmWindowMinutes INTEGER NOT NULL DEFAULT 30")
                // F13: Holiday auto-skip
                db.execSQL("ALTER TABLE alarms ADD COLUMN skipOnHolidays INTEGER NOT NULL DEFAULT 0")
                // F2: NFC tag challenge
                db.execSQL("ALTER TABLE alarms ADD COLUMN nfcTagId TEXT NOT NULL DEFAULT ''")
                // F1: Barcode/QR challenge
                db.execSQL("ALTER TABLE alarms ADD COLUMN barcodeValue TEXT NOT NULL DEFAULT ''")
                // F14: Spotify ringtone
                db.execSQL("ALTER TABLE alarms ADD COLUMN spotifyUri TEXT NOT NULL DEFAULT ''")
                // F15: Philips Hue sunrise
                db.execSQL("ALTER TABLE alarms ADD COLUMN hueEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN huePreWakeMinutes INTEGER NOT NULL DEFAULT 30")
                // F16: Photo match challenge
                db.execSQL("ALTER TABLE alarms ADD COLUMN photoMatchUri TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Mission chaining
                db.execSQL("ALTER TABLE alarms ADD COLUMN challengeChain TEXT NOT NULL DEFAULT ''")
                // Progressive snooze
                db.execSQL("ALTER TABLE alarms ADD COLUMN progressiveSnooze INTEGER NOT NULL DEFAULT 0")
                // Backup sound escalation
                db.execSQL("ALTER TABLE alarms ADD COLUMN backupSoundEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN backupSoundDelaySec INTEGER NOT NULL DEFAULT 40")
                // Sunrise simulation
                db.execSQL("ALTER TABLE alarms ADD COLUMN sunriseSimulation INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN sunriseMinutes INTEGER NOT NULL DEFAULT 15")
                // Date-specific alarm
                db.execSQL("ALTER TABLE alarms ADD COLUMN specificDate TEXT NOT NULL DEFAULT ''")
                // Alarm profile
                db.execSQL("ALTER TABLE alarms ADD COLUMN profileName TEXT NOT NULL DEFAULT ''")
                // Early dismiss
                db.execSQL("ALTER TABLE alarms ADD COLUMN earlyDismissMinutes INTEGER NOT NULL DEFAULT 0")
                // Guardian Angel
                db.execSQL("ALTER TABLE alarms ADD COLUMN guardianEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN guardianPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alarms ADD COLUMN guardianDelaySec INTEGER NOT NULL DEFAULT 300")
                // Location dismiss
                db.execSQL("ALTER TABLE alarms ADD COLUMN locationDismissEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN locationDismissLat REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN locationDismissLng REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN locationDismissRadius INTEGER NOT NULL DEFAULT 100")
                // Wi-Fi dismiss
                db.execSQL("ALTER TABLE alarms ADD COLUMN wifiDismissSsid TEXT NOT NULL DEFAULT ''")
                // Internet radio
                db.execSQL("ALTER TABLE alarms ADD COLUMN internetRadioUrl TEXT NOT NULL DEFAULT ''")
                // Flashlight strobe
                db.execSQL("ALTER TABLE alarms ADD COLUMN flashlightStrobe INTEGER NOT NULL DEFAULT 0")
                // Morning routine
                db.execSQL("ALTER TABLE alarms ADD COLUMN morningRoutine TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1.4.0: Hardware button action (NONE/SNOOZE/DISMISS)
                db.execSQL("ALTER TABLE alarms ADD COLUMN hardwareButtonAction TEXT NOT NULL DEFAULT 'NONE'")
                // v1.4.0: Auto-dismiss when the ringtone/track finishes naturally
                db.execSQL("ALTER TABLE alarms ADD COLUMN dismissAtRingtoneEnd INTEGER NOT NULL DEFAULT 0")
                // v1.4.0: Random ringtone pool (comma-separated URIs)
                db.execSQL("ALTER TABLE alarms ADD COLUMN ringtonePool TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1.5.0: Sunrise/sunset-relative firing (minutes offset, anchor)
                db.execSQL("ALTER TABLE alarms ADD COLUMN solarOffsetMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN solarAnchor TEXT NOT NULL DEFAULT 'SUNRISE'")
            }
        }
    }
}
