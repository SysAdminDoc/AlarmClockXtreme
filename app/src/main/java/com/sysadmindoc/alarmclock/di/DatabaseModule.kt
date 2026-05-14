package com.sysadmindoc.alarmclock.di

import android.content.Context
import androidx.room.Room
import com.sysadmindoc.alarmclock.data.local.AlarmDao
import com.sysadmindoc.alarmclock.data.local.AlarmDatabase
import com.sysadmindoc.alarmclock.data.local.AlarmEventDao
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
            .addMigrations(
                AlarmDatabase.MIGRATION_1_2,
                AlarmDatabase.MIGRATION_2_3,
                AlarmDatabase.MIGRATION_3_4,
                AlarmDatabase.MIGRATION_4_5,
                AlarmDatabase.MIGRATION_5_6,
                AlarmDatabase.MIGRATION_6_7,
                AlarmDatabase.MIGRATION_7_8,
                AlarmDatabase.MIGRATION_8_9,
            )
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
}
