pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
            }
            if (requested.id.id == "org.jetbrains.kotlin.kapt") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver") version "0.4.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "Ieee Connect"
include(":app")
