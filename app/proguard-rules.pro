# AlarmClockXtreme v0.8.1 ProGuard / R8 Rules
# Validated for: Hilt, Moshi (codegen-only), Retrofit, Room, Glance, Compose

# ===== Room =====
-keep class com.sysadmindoc.alarmclock.data.model.** { *; }
-keep class com.sysadmindoc.alarmclock.data.local.entity.** { *; }
-keep class com.sysadmindoc.alarmclock.data.local.AlarmDatabase { *; }

# ===== Hilt / Dagger =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class *

# ===== BroadcastReceivers & Services (manifest-referenced) =====
-keep class com.sysadmindoc.alarmclock.receiver.** { *; }
-keep class com.sysadmindoc.alarmclock.service.** { *; }

# ===== Moshi (codegen only - no reflection adapter) =====
-keep class com.sysadmindoc.alarmclock.data.remote.** { *; }
-keep class com.sysadmindoc.alarmclock.data.backup.AlarmBackup { *; }
-keep class com.sysadmindoc.alarmclock.data.backup.BackupData { *; }
-keep class com.sysadmindoc.alarmclock.data.backup.SettingsBackup { *; }
# Keep generated JsonAdapter classes
-keep class **JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keep class com.squareup.moshi.JsonAdapter
-keep class * extends com.squareup.moshi.JsonAdapter

# ===== Retrofit =====
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ===== Kotlin / Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ===== Compose =====
-dontwarn androidx.compose.**

# ===== Glance Widget =====
-keep class com.sysadmindoc.alarmclock.widget.** { *; }
-keep class androidx.glance.** { *; }

# ===== Application class =====
-keep class com.sysadmindoc.alarmclock.AlarmClockApp { *; }

# ===== Preferences DataStore =====
-keep class com.sysadmindoc.alarmclock.data.preferences.** { *; }
