package com.sysadmindoc.alarmclock.di

import com.sysadmindoc.alarmclock.data.remote.AirQualityApi
import com.sysadmindoc.alarmclock.data.remote.GeocodingApi
import com.sysadmindoc.alarmclock.data.remote.HolidayApi
import com.sysadmindoc.alarmclock.data.remote.WeatherAlertsApi
import com.sysadmindoc.alarmclock.data.remote.WeatherApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideWeatherApi(moshi: Moshi, client: OkHttpClient): WeatherApi {
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAirQualityApi(moshi: Moshi, client: OkHttpClient): AirQualityApi {
        return Retrofit.Builder()
            .baseUrl("https://air-quality-api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AirQualityApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeocodingApi(moshi: Moshi, client: OkHttpClient): GeocodingApi {
        return Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeocodingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideHolidayApi(moshi: Moshi, client: OkHttpClient): HolidayApi {
        return Retrofit.Builder()
            .baseUrl("https://date.nager.at/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HolidayApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherAlertsApi(moshi: Moshi, client: OkHttpClient): WeatherAlertsApi {
        // NWS endpoint — US-only. Returns empty `features` outside the US,
        // so it's safe to call unconditionally.
        return Retrofit.Builder()
            .baseUrl("https://api.weather.gov/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherAlertsApi::class.java)
    }
}
