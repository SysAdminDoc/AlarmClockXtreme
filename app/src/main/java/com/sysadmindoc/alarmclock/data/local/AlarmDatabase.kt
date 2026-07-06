package com.sysadmindoc.alarmclock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sysadmindoc.alarmclock.data.local.entity.ActigraphySession
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.data.local.entity.AlarmEvent
import com.sysadmindoc.alarmclock.data.local.entity.SnoreEvent
import com.sysadmindoc.alarmclock.data.model.Alarm

@Database(
    entities = [
        Alarm::class,
        AlarmEvent::class,
        ActigraphySession::class,
        AlarmIncidentEvent::class,
        SnoreEvent::class
    ],
    version = 20,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun alarmEventDao(): AlarmEventDao
    abstract fun actigraphySessionDao(): ActigraphySessionDao
    abstract fun alarmIncidentEventDao(): AlarmIncidentEventDao
    abstract fun snoreEventDao(): SnoreEventDao

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

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1.10.3: Per-alarm deliberate hold confirmation for dismiss.
                db.execSQL("ALTER TABLE alarms ADD COLUMN holdToDismissEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1.12.0 (roadmap N7): Per-alarm vibration start-delay so
                // users can pair a long gradualVolumeSeconds fade-in with
                // late-arriving haptics for a "gentle wake" preset.
                db.execSQL("ALTER TABLE alarms ADD COLUMN vibrationDelaySeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1.13.7 (roadmap X6): persist challenge retry counts so the
                // Statistics screen can correlate wake friction with sleep.
                db.execSQL("ALTER TABLE alarm_events ADD COLUMN challengeRetryCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v1.13.8 (roadmap X2): compact smart-window actigraphy
                // summaries. Raw accelerometer samples are intentionally not
                // persisted.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS actigraphy_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        alarmId INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER NOT NULL,
                        targetTime INTEGER NOT NULL,
                        totalMinutes INTEGER NOT NULL,
                        awakeMinutes INTEGER NOT NULL,
                        lightMinutes INTEGER NOT NULL,
                        deepMinutes INTEGER NOT NULL,
                        averageSleepIndex REAL NOT NULL,
                        firedEarly INTEGER NOT NULL,
                        algorithm TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE actigraphy_sessions ADD COLUMN decisionReason TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE actigraphy_sessions ADD COLUMN observedMinutesBeforeDecision INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE actigraphy_sessions ADD COLUMN smartWakeMode TEXT NOT NULL DEFAULT 'CONSERVATIVE'")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS alarm_incident_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fireId TEXT NOT NULL,
                        alarmId INTEGER NOT NULL,
                        scheduledAt INTEGER NOT NULL,
                        eventAt INTEGER NOT NULL,
                        elapsedMs INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        status TEXT NOT NULL,
                        reasonCode TEXT NOT NULL,
                        source TEXT NOT NULL,
                        sdkInt INTEGER NOT NULL,
                        standbyBucket TEXT NOT NULL,
                        exactAlarmAllowed TEXT NOT NULL,
                        notificationPermissionGranted TEXT NOT NULL,
                        fullScreenIntentAllowed TEXT NOT NULL,
                        batteryOptimizationsIgnored TEXT NOT NULL,
                        algorithmVersion TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_alarm_incident_events_alarmId ON alarm_incident_events(alarmId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_alarm_incident_events_fireId ON alarm_incident_events(fireId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_alarm_incident_events_eventAt ON alarm_incident_events(eventAt)")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN weatherEarlyMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN requiredSquats INTEGER NOT NULL DEFAULT 10")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN dismissActionType TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE alarms ADD COLUMN dismissActionPayload TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN firingBackgroundImageEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN firingBackgroundImageUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alarms ADD COLUMN firingBackgroundBlurEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE alarms
                    SET sortOrder = (
                        SELECT COUNT(*) * 1000
                        FROM alarms AS ranked
                        WHERE ranked.hour < alarms.hour
                            OR (ranked.hour = alarms.hour AND ranked.minute < alarms.minute)
                            OR (
                                ranked.hour = alarms.hour
                                    AND ranked.minute = alarms.minute
                                    AND ranked.id <= alarms.id
                            )
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS snore_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionStartedAt INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER NOT NULL,
                        durationMillis INTEGER NOT NULL,
                        peakDb REAL NOT NULL,
                        averageDb REAL NOT NULL,
                        windowCount INTEGER NOT NULL,
                        source TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_snore_events_sessionStartedAt ON snore_events(sessionStartedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_snore_events_startedAt ON snore_events(startedAt)")
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
        )
    }
}
