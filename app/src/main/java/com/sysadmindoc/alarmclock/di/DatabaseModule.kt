package com.sysadmindoc.alarmclock.di

import android.content.Context
import androidx.room.Room
import com.sysadmindoc.alarmclock.data.local.ActigraphySessionDao
import com.sysadmindoc.alarmclock.data.local.AlarmDao
import com.sysadmindoc.alarmclock.data.local.AlarmDatabase
import com.sysadmindoc.alarmclock.data.local.AlarmEventDao
import com.sysadmindoc.alarmclock.data.local.AlarmIncidentEventDao
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
}
