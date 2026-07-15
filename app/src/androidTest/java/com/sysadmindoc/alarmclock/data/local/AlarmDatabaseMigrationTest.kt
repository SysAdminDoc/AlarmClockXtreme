package com.sysadmindoc.alarmclock.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AlarmDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlarmDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratesEarliestExportedSchemaToLatestAndPreservesAlarmRows() {
        var db = helper.createDatabase("migration-4-to-latest.db", 4)
        insertSyntheticAlarm(db)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-4-to-latest.db",
            LATEST_SCHEMA_VERSION,
            true,
            *AlarmDatabase.ALL_MIGRATIONS.fromVersion(4),
        )

        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM alarms"))
        assertEquals(
            "Migration alarm",
            db.queryString("SELECT label FROM alarms LIMIT 1"),
        )
        assertEquals(0, db.queryLong("SELECT vibrationDelaySeconds FROM alarms LIMIT 1"))
        assertEquals(0, db.queryLong("SELECT firingBackgroundImageEnabled FROM alarms LIMIT 1"))
        assertEquals("", db.queryString("SELECT firingBackgroundImageUri FROM alarms LIMIT 1"))
        assertEquals(1, db.queryLong("SELECT firingBackgroundBlurEnabled FROM alarms LIMIT 1"))
        assertEquals(1000, db.queryLong("SELECT sortOrder FROM alarms LIMIT 1"))
        assertEquals("", db.queryString("SELECT shiftPattern FROM alarms LIMIT 1"))
        assertEquals("", db.queryString("SELECT shiftPatternStartDate FROM alarms LIMIT 1"))
        assertEquals("LOCAL", db.queryString("SELECT timezonePolicy FROM alarms LIMIT 1"))
        assertEquals("", db.queryString("SELECT fixedTimezoneId FROM alarms LIMIT 1"))
        db.close()
    }

    @Test
    fun migrationNineToTenAddsVibrationDelayDefaultZero() {
        var db = helper.createDatabase("migration-9-to-10.db", 9)
        insertSyntheticAlarm(db)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-9-to-10.db",
            10,
            true,
            AlarmDatabase.MIGRATION_9_10,
        )

        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM alarms"))
        assertEquals(0, db.queryLong("SELECT vibrationDelaySeconds FROM alarms LIMIT 1"))
        db.close()
    }

    @Test
    fun migrationTenToElevenAddsChallengeRetryDefaultZero() {
        var db = helper.createDatabase("migration-10-to-11.db", 10)
        insertSyntheticAlarmEvent(db)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-10-to-11.db",
            11,
            true,
            AlarmDatabase.MIGRATION_10_11,
        )

        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM alarm_events"))
        assertEquals(0, db.queryLong("SELECT challengeRetryCount FROM alarm_events LIMIT 1"))
        db.close()
    }

    @Test
    fun migrationElevenToTwelveCreatesActigraphySessionsTable() {
        var db = helper.createDatabase("migration-11-to-12.db", 11)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-11-to-12.db",
            12,
            true,
            AlarmDatabase.MIGRATION_11_12,
        )

        assertEquals(0, db.queryLong("SELECT COUNT(*) FROM actigraphy_sessions"))
        val columns = tableColumns(db, "actigraphy_sessions").map { it.name }.toSet()
        assertTrue(columns.contains("alarmId"))
        assertTrue(columns.contains("averageSleepIndex"))
        assertTrue(columns.contains("firedEarly"))
        assertTrue(columns.contains("algorithm"))
        db.close()
    }

    @Test
    fun migrationTwelveToThirteenAddsSmartWakeDecisionEvidenceDefaults() {
        var db = helper.createDatabase("migration-12-to-13.db", 12)
        insertSyntheticActigraphySession(db)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-12-to-13.db",
            13,
            true,
            AlarmDatabase.MIGRATION_12_13,
        )

        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM actigraphy_sessions"))
        assertEquals("UNKNOWN", db.queryString("SELECT decisionReason FROM actigraphy_sessions LIMIT 1"))
        assertEquals(0, db.queryLong("SELECT observedMinutesBeforeDecision FROM actigraphy_sessions LIMIT 1"))
        assertEquals("CONSERVATIVE", db.queryString("SELECT smartWakeMode FROM actigraphy_sessions LIMIT 1"))
        db.close()
    }

    @Test
    fun migrationThirteenToFourteenCreatesAlarmIncidentEventsTable() {
        var db = helper.createDatabase("migration-13-to-14.db", 13)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-13-to-14.db",
            14,
            true,
            AlarmDatabase.MIGRATION_13_14,
        )

        assertEquals(0, db.queryLong("SELECT COUNT(*) FROM alarm_incident_events"))
        val columns = tableColumns(db, "alarm_incident_events").map { it.name }.toSet()
        assertTrue(columns.contains("fireId"))
        assertTrue(columns.contains("alarmId"))
        assertTrue(columns.contains("scheduledAt"))
        assertTrue(columns.contains("eventAt"))
        assertTrue(columns.contains("elapsedMs"))
        assertTrue(columns.contains("reasonCode"))
        assertTrue(columns.contains("fullScreenIntentAllowed"))
        assertTrue(columns.contains("batteryOptimizationsIgnored"))
        db.close()
    }

    @Test
    fun migrationSeventeenToEighteenAddsFiringBackgroundDefaults() {
        var db = helper.createDatabase("migration-17-to-18.db", 17)
        insertSyntheticAlarm(db)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-17-to-18.db",
            18,
            true,
            AlarmDatabase.MIGRATION_17_18,
        )

        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM alarms"))
        assertEquals(0, db.queryLong("SELECT firingBackgroundImageEnabled FROM alarms LIMIT 1"))
        assertEquals("", db.queryString("SELECT firingBackgroundImageUri FROM alarms LIMIT 1"))
        assertEquals(1, db.queryLong("SELECT firingBackgroundBlurEnabled FROM alarms LIMIT 1"))
        db.close()
    }

    @Test
    fun migrationEighteenToNineteenAddsManualSortOrderByTime() {
        var db = helper.createDatabase("migration-18-to-19.db", 18)
        insertSyntheticAlarm(db, hour = 9, minute = 15, label = "Late")
        insertSyntheticAlarm(db, hour = 6, minute = 30, label = "Early")
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-18-to-19.db",
            19,
            true,
            AlarmDatabase.MIGRATION_18_19,
        )

        assertEquals(2, db.queryLong("SELECT COUNT(*) FROM alarms"))
        assertEquals("Early", db.queryString("SELECT label FROM alarms ORDER BY sortOrder ASC LIMIT 1"))
        assertEquals(1000, db.queryLong("SELECT sortOrder FROM alarms WHERE label = 'Early'"))
        assertEquals(2000, db.queryLong("SELECT sortOrder FROM alarms WHERE label = 'Late'"))
        db.close()
    }

    @Test
    fun migrationNineteenToTwentyCreatesSnoreEventsTable() {
        var db = helper.createDatabase("migration-19-to-20.db", 19)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-19-to-20.db",
            20,
            true,
            AlarmDatabase.MIGRATION_19_20,
        )

        assertEquals(0, db.queryLong("SELECT COUNT(*) FROM snore_events"))
        val columns = tableColumns(db, "snore_events").map { it.name }.toSet()
        assertTrue(columns.contains("sessionStartedAt"))
        assertTrue(columns.contains("startedAt"))
        assertTrue(columns.contains("durationMillis"))
        assertTrue(columns.contains("peakDb"))
        assertTrue(columns.contains("averageDb"))
        db.close()
    }

    @Test
    fun migrationTwentyToTwentyOneCreatesPreSleepTagEntriesTable() {
        var db = helper.createDatabase("migration-20-to-21.db", 20)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-20-to-21.db",
            21,
            true,
            AlarmDatabase.MIGRATION_20_21,
        )

        assertEquals(0, db.queryLong("SELECT COUNT(*) FROM pre_sleep_tag_entries"))
        val columns = tableColumns(db, "pre_sleep_tag_entries").associateBy { it.name }
        assertTrue(columns.containsKey("localDate"))
        assertTrue(columns.containsKey("tagKey"))
        assertTrue(columns.containsKey("loggedAt"))
        assertEquals(1, columns.getValue("localDate").primaryKeyPosition)
        assertEquals(2, columns.getValue("tagKey").primaryKeyPosition)
        db.close()
    }

    @Test
    fun migrationTwentyOneToTwentyTwoAddsShiftPatternDefaults() {
        var db = helper.createDatabase("migration-21-to-22.db", 21)
        insertSyntheticAlarm(db)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-21-to-22.db",
            22,
            true,
            AlarmDatabase.MIGRATION_21_22,
        )

        assertEquals(1, db.queryLong("SELECT COUNT(*) FROM alarms"))
        assertEquals("", db.queryString("SELECT shiftPattern FROM alarms LIMIT 1"))
        assertEquals("", db.queryString("SELECT shiftPatternStartDate FROM alarms LIMIT 1"))
        db.close()
    }

    @Test
    fun migrationTwentyTwoToTwentyThreeAddsLocalTimezoneDefaults() {
        var db = helper.createDatabase("migration-22-to-23.db", 22)
        insertSyntheticAlarm(db)
        db.close()

        db = helper.runMigrationsAndValidate(
            "migration-22-to-23.db",
            23,
            true,
            AlarmDatabase.MIGRATION_22_23,
        )

        assertEquals("LOCAL", db.queryString("SELECT timezonePolicy FROM alarms LIMIT 1"))
        assertEquals("", db.queryString("SELECT fixedTimezoneId FROM alarms LIMIT 1"))
        db.close()
    }

    @Test
    fun freshInstallVersionMatchesLatestExportedSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val exportedLatest = latestExportedSchemaVersion()
        val database = Room.inMemoryDatabaseBuilder(context, AlarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        assertEquals(exportedLatest, database.openHelper.writableDatabase.version)
        database.close()
    }

    @Test
    fun everyCommittedMigrationIsContiguousThroughLatestSchema() {
        val migrations = AlarmDatabase.ALL_MIGRATIONS.sortedBy { it.startVersion }
        assertEquals(1, migrations.first().startVersion)
        migrations.zipWithNext().forEach { (left, right) ->
            assertEquals(left.endVersion, right.startVersion)
        }
        assertEquals(LATEST_SCHEMA_VERSION, migrations.last().endVersion)
    }

    private fun insertSyntheticAlarm(
        db: SupportSQLiteDatabase,
        hour: Int = 6,
        minute: Int = 30,
        label: String = "Migration alarm"
    ) {
        val columns = tableColumns(db, "alarms")
            .filterNot { it.primaryKeyPosition > 0 && it.name == "id" }
        val columnSql = columns.joinToString(", ") { "`${it.name}`" }
        val placeholders = columns.joinToString(", ") { "?" }
        val values = columns.map { syntheticValueFor(it, hour, minute, label) }.toTypedArray()

        db.execSQL(
            "INSERT INTO alarms ($columnSql) VALUES ($placeholders)",
            values,
        )
    }

    private fun insertSyntheticAlarmEvent(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                INSERT INTO alarm_events (
                    alarmId,
                    alarmLabel,
                    scheduledTime,
                    firedAt,
                    action,
                    actionAt,
                    challengeType,
                    challengeSolveTimeMs,
                    snoozeCount,
                    dayOfWeek
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any>(
                1L,
                "Migration event",
                1_700_000_000_000L,
                1_700_000_000_000L,
                "DISMISSED",
                1_700_000_060_000L,
                "MATH_EASY",
                60_000L,
                1,
                1,
            ),
        )
    }

    private fun insertSyntheticActigraphySession(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                INSERT INTO actigraphy_sessions (
                    alarmId,
                    startedAt,
                    endedAt,
                    targetTime,
                    totalMinutes,
                    awakeMinutes,
                    lightMinutes,
                    deepMinutes,
                    averageSleepIndex,
                    firedEarly,
                    algorithm
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any>(
                1L,
                1_700_000_000_000L,
                1_700_000_600_000L,
                1_700_000_900_000L,
                10,
                1,
                6,
                3,
                0.42,
                0,
                "phone_cole_kripke_experimental_v1",
            ),
        )
    }

    private fun tableColumns(db: SupportSQLiteDatabase, table: String): List<TableColumn> {
        return db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val pkIndex = cursor.getColumnIndexOrThrow("pk")
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        TableColumn(
                            name = cursor.getString(nameIndex),
                            type = cursor.getString(typeIndex),
                            primaryKeyPosition = cursor.getInt(pkIndex),
                        )
                    )
                }
            }
        }
    }

    private fun syntheticValueFor(
        column: TableColumn,
        hour: Int,
        minute: Int,
        label: String
    ): Any {
        return when (column.name) {
            "hour" -> hour
            "minute" -> minute
            "label" -> label
            "isEnabled", "vibrationEnabled", "showOnLockScreen" -> 1
            "createdAt", "nextTriggerTime" -> 1_700_000_000_000L
            "repeatDays" -> "MONDAY,TUESDAY,WEDNESDAY"
            "challengeType" -> "NONE"
            "vibrationPattern" -> "default"
            "solarAnchor" -> "SUNRISE"
            "hardwareButtonAction" -> "NONE"
            else -> when {
                column.type.contains("INT", ignoreCase = true) -> 0
                column.type.contains("REAL", ignoreCase = true) -> 0.0
                else -> ""
            }
        }
    }

    private fun latestExportedSchemaVersion(): Int {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val schemaDir = requireNotNull(AlarmDatabase::class.qualifiedName)
        val versions = assets.list(schemaDir)
            ?.mapNotNull { fileName ->
                fileName.removeSuffix(".json").toIntOrNull()
            }
            .orEmpty()

        assertTrue("No exported Room schemas found in androidTest assets", versions.isNotEmpty())
        val latest = versions.max()
        assets.open(File(schemaDir, "$latest.json").path).use { input ->
            assertTrue("Latest exported schema is empty", input.read() >= 0)
        }
        return latest
    }

    private fun SupportSQLiteDatabase.queryLong(sql: String): Long {
        return query(sql).use { cursor ->
            assertTrue("Expected one row for query: $sql", cursor.moveToFirst())
            cursor.getLong(0)
        }
    }

    private fun SupportSQLiteDatabase.queryString(sql: String): String {
        return query(sql).use { cursor ->
            assertTrue("Expected one row for query: $sql", cursor.moveToFirst())
            cursor.getString(0)
        }
    }

    private fun Array<androidx.room.migration.Migration>.fromVersion(
        startVersion: Int,
    ): Array<androidx.room.migration.Migration> {
        return filter { it.startVersion >= startVersion }.toTypedArray()
    }

    private data class TableColumn(
        val name: String,
        val type: String,
        val primaryKeyPosition: Int,
    )

    private companion object {
        const val LATEST_SCHEMA_VERSION = 23
    }
}
