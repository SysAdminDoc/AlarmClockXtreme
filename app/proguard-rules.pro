# AlarmClockXtreme v1.5.2 ProGuard / R8 Rules
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

# ===== Direct Boot fallback path =====
# These classes are the minimal pre-unlock alarm path. Keep them explicitly so
# release shrinking cannot break Direct Boot if manifest wiring is refactored.
-keep class com.sysadmindoc.alarmclock.directboot.** { *; }

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

# ===== Challenge types — enum names are persisted in Room (challengeType /
#       challengeChain columns) and restored via ChallengeType.valueOf().
#       R8 must not rename these constants or valueOf() will silently fail. =====
-keep enum com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ChallengeType { *; }

# ===== Workers =====
-keep @androidx.hilt.work.HiltWorker class * { *; }

# ===== NewPipe Extractor / Mozilla Rhino (v1.7.5) =====
# RhinoScriptEngineFactory references javax.script.* which doesn't exist on
# Android. NewPipe never actually loads the script-engine factory at runtime
# on Android, so suppressing the warning is safe.
-dontwarn javax.script.**
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.javascript.tools.**
-dontwarn org.schabi.newpipe.extractor.**
# Keep Mozilla Rhino's reflection-loaded classes — NewPipe Extractor uses
# Rhino to evaluate YouTube's signature-decoding JavaScript.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
# NewPipe extractor uses Jsoup; keep its public API.
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ===== Play downloader transitive hardening =====
# Commons Compress references optional Zstandard classes; the app's downloader
# path does not create Zstandard streams, and adding zstd-jni would ship native
# code that is not needed for alarm audio downloads.
-dontwarn com.github.luben.zstd.**
