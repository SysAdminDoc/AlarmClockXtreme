// AlarmClockXtreme v1.15.28
// Top-level build file
plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
}

val resolveDependencyIntegrity = tasks.register("resolveDependencyIntegrity") {
    group = "verification"
    description = "Resolve release graphs and tests used to refresh verification metadata/locks."
}
val verifyDependencyIntegrity = tasks.register("verifyDependencyIntegrity") {
    group = "verification"
    description = "Verify checksums and locked Play, F-Droid, and Wear release graphs."
}
val verifyStableReleaseDependencies = tasks.register("verifyStableReleaseDependencies") {
    group = "verification"
    description = "Reject alpha, beta, and release-candidate versions in release lockfiles."
    val releaseLockfiles = files("app/gradle.lockfile", "wear/gradle.lockfile")
    inputs.files(releaseLockfiles)
    doLast {
        val prerelease = Regex("(?:^|[-.])(alpha|beta|rc)\\d*(?:$|[-.])", RegexOption.IGNORE_CASE)
        val violations = releaseLockfiles.files.flatMap { lockfile ->
            lockfile.readLines().mapNotNull { line ->
                val coordinate = line.substringBefore('=').trim()
                val version = coordinate.substringAfterLast(':', missingDelimiterValue = "")
                if (version.isNotEmpty() && prerelease.containsMatchIn(version)) {
                    "${lockfile.relativeTo(rootDir)}: $coordinate"
                } else {
                    null
                }
            }
        }
        check(violations.isEmpty()) {
            "Pre-release dependencies are not allowed in release graphs:\n" + violations.joinToString("\n")
        }
    }
}
verifyDependencyIntegrity.configure { dependsOn(verifyStableReleaseDependencies) }

subprojects {
    val releaseLockConfigurations = setOf(
        "playReleaseRuntimeClasspath",
        "fdroidReleaseRuntimeClasspath",
        "releaseRuntimeClasspath"
    )

    dependencyLocking {
        lockMode.set(org.gradle.api.artifacts.dsl.LockMode.STRICT)
    }
    configurations.configureEach {
        if (name in releaseLockConfigurations) {
            resolutionStrategy.activateDependencyLocking()
        }
    }

    fun resolveReleaseGraphs() {
        releaseLockConfigurations
            .mapNotNull(configurations::findByName)
            .sortedBy { it.name }
            .forEach { configuration ->
                require(configuration.isCanBeResolved) {
                    "${project.path}:${configuration.name} is not resolvable."
                }
                configuration.resolve()
            }
    }

    val projectVerification = tasks.register("verifyDependencyIntegrity") {
        group = "verification"
        description = "Verify $path release runtime dependency checksums and locks."
        doLast { resolveReleaseGraphs() }
    }
    verifyDependencyIntegrity.configure { dependsOn(projectVerification) }

    val projectIntegrity = tasks.register("resolveDependencyIntegrity") {
        group = "verification"
        description = "Resolve $path release runtime configurations for dependency locking."
        doFirst {
            require(gradle.startParameter.isWriteDependencyLocks) {
                "Run with --write-locks so release dependency locks are updated intentionally."
            }
        }
        doLast { resolveReleaseGraphs() }
    }
    resolveDependencyIntegrity.configure { dependsOn(projectIntegrity) }
}
