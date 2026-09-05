import java.util.Properties
import java.io.FileInputStream
import org.gradle.api.GradleException

// AlarmClockXtreme v1.15.35
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
        versionCode = 137
        versionName = "1.15.35"

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

    testOptions {
        unitTests {
            // Robolectric needs the merged manifest/resources to bootstrap.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        error += "HardcodedText"
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

val releaseArtifactTasks = setOf(
    "assembleRelease",
    "assemblePlayRelease",
    "bundleRelease",
    "bundlePlayRelease",
    "assembleFdroidRelease",
    "bundleFdroidRelease"
)

tasks.matching { it.name in releaseArtifactTasks }.configureEach {
    dependsOn(rootProject.tasks.named("verifyReleaseSigning"))
    dependsOn(rootProject.tasks.named("verifyReleaseMetadata"))
}

val fdroidReleaseApk = layout.buildDirectory.file("outputs/apk/fdroid/release/app-fdroid-release.apk")
val verifyFdroidReleaseSize by tasks.registering {
    group = "verification"
    description = "Keep the F-Droid release APK below the documented 40 MiB budget."
    dependsOn("assembleFdroidRelease")
    inputs.file(fdroidReleaseApk)

    doLast {
        val apk = fdroidReleaseApk.get().asFile
        check(apk.isFile) {
            "F-Droid release APK was not produced: ${apk.path}"
        }
        val maxBytes = 40L * 1024L * 1024L
        check(apk.length() <= maxBytes) {
            "F-Droid release APK is ${apk.length() / (1024.0 * 1024.0)} MiB; maximum is 40 MiB."
        }
    }
}

val verifyRoomSchemaExports by tasks.registering {
    group = "verification"
    description = "Reject Room schema exports changed by a debug build until they are reviewed and committed."
    dependsOn("kspFdroidDebugKotlin", "kspPlayDebugKotlin")

    doLast {
        fun runGit(vararg arguments: String): Pair<Int, String> {
            val process = ProcessBuilder(listOf("git") + arguments.toList())
                .directory(rootProject.rootDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            return process.waitFor() to output
        }

        val (workingTreeExit, workingTreeDiff) = runGit("diff", "--exit-code", "--", "app/schemas")
        check(workingTreeExit == 0) {
            "Room schema exports changed after the debug build. Review and commit app/schemas before continuing.\n$workingTreeDiff"
        }
        val (stagedExit, stagedDiff) = runGit("diff", "--cached", "--exit-code", "--", "app/schemas")
        check(stagedExit == 0) {
            "Room schema exports are staged but not committed. Review and commit app/schemas before continuing.\n$stagedDiff"
        }
        val (statusExit, status) = runGit("status", "--short", "--untracked-files=all", "--", "app/schemas")
        check(statusExit == 0 && status.isBlank()) {
            "Room schema exports contain untracked changes. Review and commit app/schemas before continuing.\n$status"
        }
    }
}

/**
 * The two files the guard skips, and why each one is not a translation gap.
 *
 * This started at 30 files on 2026-08-22 when the guard was pointed at the
 * whole app package instead of just `ui/`. Everything else on it has been
 * drained. What is left is English that is supposed to stay English:
 *
 *  - SupportExportManager writes the crash-log section of the support bundle,
 *    which a maintainer reads. The rest of that bundle is English headings and
 *    reason codes for the same reason.
 *  - WebhookService builds a sample JSON payload ("Test Alarm", "12:00 PM")
 *    that goes to somebody else's endpoint. Translating a wire value would
 *    break the consumer, not localise it.
 *
 * Adding a third entry means shipping English a translator cannot reach, so it
 * needs a reason written here, not just a filename.
 */
val unlocalizedComposeFiles = setOf(
    "data/support/SupportExportManager.kt",
    "service/WebhookService.kt"
)

val primaryComposeScreenFiles: List<File> = fileTree("src/main/java/com/sysadmindoc/alarmclock") {
    include("**/*.kt")
}.files
    .sortedBy { it.path }
    .filterNot { candidate ->
        unlocalizedComposeFiles.any { suffix ->
            candidate.invariantSeparatorsPath.endsWith(suffix)
        }
    }

val verifyLocalizedPrimaryScreens by tasks.registering {
    group = "verification"
    // Deliberately narrow: this rejects literals in the shapes listed below, not
    // "all hardcoded text". It cannot tell a `when` arm feeding a Text from one
    // feeding an API query parameter, and it does not follow a string through a
    // local val. Say what it does, so nobody reads a green run as proof the
    // screens hold no English.
    description = "Rejects literals passed to Text(), to a known text attribute, " +
        "to a Toast or a snackbar, or returned from a branch or a return statement " +
        "that reads like copy, anywhere in the app package except the files listed " +
        "in unlocalizedComposeFiles."
    inputs.files(primaryComposeScreenFiles)

    doLast {
        // Attributes that carry text a person reads. An earlier version of this
        // task anchored the literal to `[A-Za-z]`, which let every interpolated
        // string ("$wins CPU") through, and only looked at a handful of
        // attribute names.
        val uiTextAttributes = listOf(
            "text", "contentDescription", "title", "description", "supportingText",
            "onClickLabel", "stateDescription", "overline", "actionLabel", "summary",
            "statusLabel", "subtitle", "headline", "message", "placeholder", "hint",
            "caption", "helperText", "errorText", "emptyText", "value"
        ).joinToString("|")
        // Assigning a literal to one of those attributes is unambiguously UI.
        // `\bText\(` needs a word boundary, so a composable whose name merely
        // ends in "Text" (ChallengeSupportText) used to slip past; match any
        // identifier ending in Text instead.
        val directUiLiteralPatterns = listOf(
            Regex("""\b[A-Za-z]*Text\s*\(\s*"([^"\r\n]*)""""),
            Regex("""\b(?:$uiTextAttributes)\s*=\s*"([^"\r\n]*)""""),
            // Text that never touches a Compose attribute but is still read.
            Regex("""Toast\.makeText\s*\([^,]*,\s*"([^"\r\n]*)""""),
            Regex("""\bshowSnackbar\s*\(\s*"([^"\r\n]*)""""),
            // Notification and tile builders. These are setter calls, not
            // assignments, so neither pattern above could see them, and every
            // one of them is a line on the lock screen.
            Regex(
                """\.set(?:ContentTitle|ContentText|SubText|Ticker|ContentDescription)""" +
                    """\s*\(\s*"([^"\r\n]*)""""
            )
        )
        // `text = if (x) "A" else "B"` and `Outcome.WIN -> "You won"`: the
        // literal never sits directly after the `=`, so the patterns above
        // cannot see it. Nothing here says whether the branch feeds a Text or
        // an API query parameter, so these only fire on something that reads
        // like copy: a capitalised word, or more than one word. That lets a
        // lowercase single-word label through ("low", "moderate"), which is the
        // price of not flagging every "celsius"/"kmh"/"unknown" wire value.
        // Two shapes the guard was blind to until 2026-08-22, each of
        // which had shipped English past a green run: a lambda whose
        // entire body is a literal, and a literal assigned to a name
        // that reads like display state.
        val uiStateNames = "[A-Za-z]*(?:Status|Label|Message|Title|Hint|Summary|Caption)"
        val branchLiteralPatterns = listOf(
            Regex("""(?:->|\belse\b|\?|:)\s*"([^"\r\n]*)"\s*(?:\r?\n|,|\)|\})"""),
            Regex("""\bif\s*\([^()]*(?:\([^()]*\)[^()]*)*\)\s*"([^"\r\n]*)""""),
            // `return "Next occurrence: $x"`. A helper that hands a sentence
            // back to its caller is the same violation as one that passes it to
            // Text(); this shape was missed until 2026-08-22 and hid the alarm
            // card's next-occurrence line, four Bedtime room-noise labels and
            // the voice and handwriting challenge statuses.
            Regex("""\breturn(?:@[A-Za-z_][A-Za-z0-9_]*)?\s+"([^"\r\n]*)""""),
            // `ifBlank { "Alarm details" }`: a fallback whose whole body is
            // copy. Deliberately only ifBlank/ifEmpty, not any lambda: a
            // bare `{ "..." }` also matches every require/check message,
            // which is a developer diagnostic and not a translator's
            // problem.
            Regex("""\bif(?:Blank|Empty)\s*\{\s*"([^"\r\n]*)"\s*\}"""),
            // `localStatus = "Listening."`: a local a Text reads later.
            Regex("""\b$uiStateNames\s*=\s*"([^"\r\n]*)"""")
        )
        val looksLikeCopy = Regex("""^[A-Z].*|.*\s.*""")
        // Animation debug names passed as `label = ...` to animateFloat and
        // rememberInfiniteTransition. They never reach a user, and translating
        // them would be meaningless. Checked only against `label =`, so a real
        // `Text("funnel")` is still a violation.
        val nonUiComposeLabels = setOf(
            "alarmPulse", "pulseScale", "pulseAlpha", "shake", "shakeAnim", "nfcPulse",
            "nfcAlpha", "sheep-drift", "sheep-drift-value", "icon_scale", "loading-card",
            "loading-alpha", "skeleton-block", "skeleton-alpha", "funnel", "funnel-rotation",
            "funnel-drift", "glowAlpha", "burnInDrift", "driftX", "driftY", "timer-pulse",
            "key-press-scale", "barcodeScan", "scanLine", "dotWidth\$index"
        )
        val animationLabelPattern = Regex("""\blabel\s*=\s*"([^"\r\n]*)"""")
        // What is left once every `$x` and `${...}` is removed: only that part
        // is English a translator would have to touch.
        val interpolation = Regex("""\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*""")
        val wireConstant = Regex("""^[A-Z0-9_]+$""")
        // A URL is an address, not copy. Translating "https://www.windy.com/..."
        // would break the radar card rather than localise it.
        val urlLiteral = Regex("""^[A-Za-z][A-Za-z0-9+.\-]*://""")
        // SimpleDateFormat / DateTimeFormatter patterns are not prose.
        val dateFormatPattern = Regex("""^[hHmMsSaEdDMyLZzGwWkKubB:./,•\s'\-]+$""")
        // A literal holding a nested one ("${String.format("%02d", n)}") cannot be
        // captured by a regex that stops at the first quote, so what these
        // patterns see is a fragment, not the string. Skip the truncation.
        val truncatedByNesting = Regex("""\$\{[^}]*$""")
        // A run of three letters is the shortest thing worth translating. It
        // also skips the "es"/"ies" suffix fragments some strings concatenate
        // for English plurals; those need a <plurals>, not a resource, and are
        // tracked separately.
        val translatableRun = Regex("""[A-Za-z]{3}""")
        val violations = mutableSetOf<String>()

        primaryComposeScreenFiles.forEach { sourceFile ->
            val source = sourceFile.readText()
            val report = { match: MatchResult, requireCopyShape: Boolean ->
                val literal = match.groupValues[1]
                val bare = interpolation.replace(literal, " ").trim()
                val isProse = translatableRun.containsMatchIn(bare) &&
                    !wireConstant.matches(bare) &&
                    !urlLiteral.containsMatchIn(bare) &&
                    !dateFormatPattern.matches(bare) &&
                    !truncatedByNesting.containsMatchIn(literal) &&
                    (!requireCopyShape || looksLikeCopy.matches(bare))
                if (isProse) {
                    val line = source.take(match.range.first).count { it == '\n' } + 1
                    violations += "${sourceFile.relativeTo(projectDir)}:$line: \"$literal\""
                }
            }
            directUiLiteralPatterns.forEach { pattern ->
                pattern.findAll(source).forEach { report(it, false) }
            }
            branchLiteralPatterns.forEach { pattern ->
                pattern.findAll(source).forEach { report(it, true) }
            }
            animationLabelPattern.findAll(source).forEach { match ->
                if (match.groupValues[1] !in nonUiComposeLabels) report(match, false)
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Hardcoded primary-screen UI text must use stringResource(...):\n" +
                    violations.sorted().joinToString("\n")
            )
        }
    }
}

tasks.matching { it.name == "check" || it.name.startsWith("lint") }.configureEach {
    dependsOn(verifyLocalizedPrimaryScreens)
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn(verifyRoomSchemaExports)
    dependsOn(rootProject.tasks.named("verifyReleaseMetadata"))
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
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

    // Quarterly freshness review 2026-07-15: NewPipe 0.26.3,
    // youtubedl-android 0.18.1, and OkHttp 5.4.0 are current stable; Moshi was
    // updated to 1.15.2. Room 2.8.4 fails this KSP1 schema processor and
    // WorkManager 2.11.2 pulls Room 2.7 into the runtime graph, so both remain
    // coupled to the tracked AGP 9 / KSP2 migration in Roadmap_Blocked.md.

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
    implementation("com.squareup.moshi:moshi:1.15.2")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
    // OkHttp (explicit — also used by WebhookService and HueSunriseWorker).
    // 5.x adds an HTTP/2 total-header-size limit (resource-exhaustion guard).
    implementation("com.squareup.okhttp3:okhttp:5.4.0")

    // Glance widget
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Coroutines (unified with the :wear module on 1.11.0)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    // Media3 / ExoPlayer is the alarm audio backend. When it cannot play,
    // startMedia3DefaultFallback drops into the MediaPlayer path in AlarmService.
    implementation("androidx.media3:media3-exoplayer:1.10.1")

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
    // ML Kit Digital Ink handwriting recognition (play flavor only). The
    // f-droid flavor binds a no-op recognizer and keeps the typed fallback.
    "playImplementation"("com.google.mlkit:digital-ink-recognition:19.0.0")
    // Commons Compress 1.28.0 references XZ stream classes during release
    // shrinking; keep the support library Play-only with the downloader graph.
    "playImplementation"("org.tukaani:xz:1.10")

    constraints {
        // v1.13.2+ (R5): youtubedl-android 0.18.1 and NewPipeExtractor 0.26.x
        // still resolve stale parser/archive transitives. Keep these as
        // constraints, not direct feature dependencies, so F-Droid remains free
        // of the Play-only downloader graph.
        "playImplementation"("com.fasterxml.jackson.core:jackson-databind:2.18.9") {
            because("CVE-2026-54512/54513 bypasses, CVE-2026-54514 SSRF, CVE-2026-54515 DoS")
        }
        "playImplementation"("com.fasterxml.jackson.core:jackson-core:2.18.9") {
            because("Keep Jackson modules aligned with constrained jackson-databind")
        }
        "playImplementation"("com.fasterxml.jackson.core:jackson-annotations:2.18.9") {
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
        "playImplementation"("org.jsoup:jsoup:1.23.2") {
            because("OSV GHSA-pmhh-3w7g-xqp8 is fixed in jsoup 1.23.1 and later")
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
    // WorkManager test harness: drives WebhookRetryWorker.doWork() in the JVM so
    // the outcome->Result / retry-cap mapping is unit-tested without a device.
    testImplementation("androidx.work:work-testing:2.9.1")
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
