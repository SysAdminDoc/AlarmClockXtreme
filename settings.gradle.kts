pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Required by youtubedl-android (play flavor only). The library itself
        // is referenced as a `playImplementation`, so f-droid builds never
        // resolve it — but the repo must still be declared here because Gradle
        // resolves dependencies for all flavors during sync.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AlarmClockXtreme"
include(":app")
