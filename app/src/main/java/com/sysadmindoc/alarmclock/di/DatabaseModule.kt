package com.sysadmindoc.alarmclock.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sysadmindoc.alarmclock.data.local.ActigraphySessionDao
import com.sysadmindoc.alarmclock.data.local.AlarmDao
import com.sysadmindoc.alarmclock.data.local.AlarmDatabase
import com.sysadmindoc.alarmclock.data.local.AlarmEventDao
import com.sysadmindoc.alarmclock.data.local.AlarmIncidentEventDao
import com.sysadmindoc.alarmclock.data.local.DatabaseDowngradeNotice
import com.sysadmindoc.alarmclock.data.local.PreSleepTagDao
import com.sysadmindoc.alarmclock.data.local.SnoreEventDao
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
    fun provideDatabase(@ApplicationContext context: Context): AlarmDatabase {
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            "alarm_clock.db"
        )
            .addMigrations(*AlarmDatabase.ALL_MIGRATIONS)
            // The manifest allows restoring a backup from any version, so a
            // database stamped by a newer build can land on an older one. Room
            // throws on open in that case and every DB-backed screen crashes;
            // starting empty is recoverable, a crash loop is not. Losing the
            // alarms quietly would be its own failure, so say so.
            .fallbackToDestructiveMigrationOnDowngrade()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    DatabaseDowngradeNotice.post(context)
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDao()
    }

    @Provides
    @Singleton
    fun provideAlarmEventDao(database: AlarmDatabase): AlarmEventDao {
        return database.alarmEventDao()
    }

    @Provides
    @Singleton
    fun provideActigraphySessionDao(database: AlarmDatabase): ActigraphySessionDao {
        return database.actigraphySessionDao()
    }

    @Provides
    @Singleton
    fun provideAlarmIncidentEventDao(database: AlarmDatabase): AlarmIncidentEventDao {
        return database.alarmIncidentEventDao()
    }

    @Provides
    @Singleton
    fun provideSnoreEventDao(database: AlarmDatabase): SnoreEventDao {
        return database.snoreEventDao()
    }

    @Provides
    @Singleton
    fun providePreSleepTagDao(database: AlarmDatabase): PreSleepTagDao {
        return database.preSleepTagDao()
    }
}
