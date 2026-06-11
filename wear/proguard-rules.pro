# Wear service entry points and the tiny shared model/cache are runtime-facing:
# the system and Play Services bind these classes by manifest component name,
# and the tile/complication callbacks are invoked through library dispatch.
-keep class com.sysadmindoc.alarmclock.wear.NextAlarmTileService { *; }
-keep class com.sysadmindoc.alarmclock.wear.NextAlarmComplicationDataSourceService { *; }
-keep class com.sysadmindoc.alarmclock.wear.WearAlarmDataListenerService { *; }
-keep class com.sysadmindoc.alarmclock.wear.WearAlarmData { *; }
-keep class com.sysadmindoc.alarmclock.wear.WearAlarmSnapshot { *; }
-keep class com.sysadmindoc.alarmclock.wear.WearAlarmStore { *; }

# Wear Tiles / ProtoLayout / complication APIs are service-provider frameworks.
# Keep callback types stable under R8 full mode so release APKs behave like
# debug builds on real watches.
-keep class androidx.wear.tiles.** { *; }
-keep class androidx.wear.protolayout.** { *; }
-keep class androidx.wear.watchface.complications.** { *; }
-keep class androidx.concurrent.futures.** { *; }
-keep class com.google.common.util.concurrent.** { *; }
-keep class com.google.android.gms.wearable.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# Coroutine service scopes and dispatchers are used from tile callbacks.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

-dontwarn androidx.wear.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.common.util.concurrent.**
