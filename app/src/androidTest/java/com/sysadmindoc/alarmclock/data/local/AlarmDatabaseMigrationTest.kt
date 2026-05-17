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

    private fun insertSyntheticAlarm(db: SupportSQLiteDatabase) {
        val columns = tableColumns(db, "alarms")
            .filterNot { it.primaryKeyPosition > 0 && it.name == "id" }
        val columnSql = columns.joinToString(", ") { "`${it.name}`" }
        val placeholders = columns.joinToString(", ") { "?" }
        val values = columns.map { syntheticValueFor(it) }.toTypedArray()

        db.execSQL(
            "INSERT INTO alarms ($columnSql) VALUES ($placeholders)",
            values,
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

    private fun syntheticValueFor(column: TableColumn): Any {
        return when (column.name) {
            "hour" -> 6
            "minute" -> 30
            "label" -> "Migration alarm"
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
        const val LATEST_SCHEMA_VERSION = 10
    }
}
