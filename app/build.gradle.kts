import java.util.Properties
import java.io.FileInputStream

// AlarmClockXtreme v1.15.1
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.sysadmindoc.alarmclock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sysadmindoc.alarmclock"
        minSdk = 26
        targetSdk = 36
        versionCode = 103
        versionName = "1.15.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    // Release signing - reads from keystore.properties (not committed to git)
    // Create keystore.properties in project root with:
    //   storeFile=path/to/keystore.jks
    //   storePassword=...
    //   keyAlias=...
    //   keyPassword=...
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            manifestPlaceholders["wearActionBridgeEnabled"] = "true"
        }
        create("fdroid") {
            dimension = "distribution"
            manifestPlaceholders["wearActionBridgeEnabled"] = "false"
        }
    }

    // Required for F-Droid reproducible builds
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // v1.7.1: yt-dlp needs `libpython.zip.so` extracted to the lib/ABI dir so
    // it can read the bundled Python source on first init. AGP 8 defaults to
    // packing native libs *inside* the APK (faster start, smaller installs)
    // but yt-dlp expects them on disk. Forcing legacy packaging is what the
    // Aura app does for the same reason.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.05.00")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.56.2")
    ksp("com.google.dagger:hilt-compiler:2.56.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    // Hilt WorkManager integration (F5, F13, F15 workers)
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // WorkManager (F5, F6, F13, F15)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Retrofit + Moshi for Open-Meteo weather API and Nager.Date holidays
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")
    // OkHttp (explicit — also used by WebhookService and HueSunriseWorker).
    // 5.x adds an HTTP/2 total-header-size limit (resource-exhaustion guard).
    implementation("com.squareup.okhttp3:okhttp:5.4.0")

    // Glance widget
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Coroutines (unified with the :wear module on 1.11.0)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // YouTube alarm-sound download (play flavor only — bundles a native Python
    // interpreter that isn't F-Droid-compatible, so the f-droid flavor uses a
    // stub implementation that returns "not available in this build"). Ported
    // from the Aura/FreeVibe app (~/repos/Aura).
    "playImplementation"("io.github.junkfood02.youtubedl-android:library:0.18.1")
    // NewPipe Extractor — drives the in-dialog YouTube search ("rooster
    // crowing alarm" -> list of short clips you can tap to download). Keep on
    // the current 0.26.x line for YouTube integrity/poToken parser fixes.
    // JitPack repo declared in settings.gradle.kts.
    "playImplementation"("com.github.teamnewpipe:NewPipeExtractor:v0.26.3")
    // Wear OS Data Layer bridge (play flavor only). F-Droid keeps the wearable
    // bridge as a no-op because Play Services is proprietary.
    "playImplementation"("com.google.android.gms:play-services-wearable:20.0.1")
    // Health Connect sleep-session reads (play flavor only). F-Droid keeps
    // this out of its dependency graph and binds a no-op repository.
    "playImplementation"("androidx.health.connect:connect-client:1.1.0")
    // Commons Compress 1.28.0 references XZ stream classes during release
    // shrinking; keep the support library Play-only with the downloader graph.
    "playImplementation"("org.tukaani:xz:1.10")

    constraints {
        // v1.13.2+ (R5): youtubedl-android 0.18.1 and NewPipeExtractor 0.26.x
        // still resolve stale parser/archive transitives. Keep these as
        // constraints, not direct feature dependencies, so F-Droid remains free
        // of the Play-only downloader graph.
        "playImplementation"("com.fasterxml.jackson.core:jackson-databind:2.18.8") {
            because("CVE-2026-54512 PTV bypass, CVE-2026-54513 array bypass, CVE-2026-54514 SSRF")
        }
        "playImplementation"("com.fasterxml.jackson.core:jackson-core:2.18.8") {
            because("Keep Jackson modules aligned with constrained jackson-databind")
        }
        "playImplementation"("com.fasterxml.jackson.core:jackson-annotations:2.18.8") {
            because("Keep Jackson modules aligned with constrained jackson-databind")
        }
        "playImplementation"("org.apache.commons:commons-compress:1.28.0") {
            because("OSV reports multiple advisories against the youtubedl-android transitive 1.12")
        }
        "playImplementation"("commons-io:commons-io:2.20.0") {
            because("OSV reports advisories against the youtubedl-android transitive 2.5")
        }
        "playImplementation"("org.mozilla:rhino:1.8.1") {
            because("OSV GHSA-3w8q-xq97-5j7x fixes the NewPipe transitive 1.8.0 in 1.8.1")
        }
        "playImplementation"("org.mozilla:rhino-engine:1.8.1") {
            because("Keep Rhino engine aligned with constrained Rhino runtime")
        }
        "playImplementation"("com.google.guava:guava:33.6.0-android") {
            because("OSV reports advisories against the Health Connect transitive 31.1-android")
        }
    }

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    // Drift guard: BackupManagerSettingsDriftTest reflects over AppSettings /
    // SettingsBackup constructor parameters so a new settings field can't ship
    // without a backup round-trip again.
    testImplementation(kotlin("reflect"))
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
