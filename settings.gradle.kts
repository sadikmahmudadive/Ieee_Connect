pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
            }
            if (requested.id.id == "org.jetbrains.kotlin.kapt") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
            }
        }
    }
}

plugins {
    // Enables automatic JDK provisioning from Foojay/Adoptium if a matching toolchain is missing.
    // Applying this plugin in settings allows Gradle to download a matching Java toolchain when needed.
    id("org.gradle.toolchains.foojay-resolver") version "0.4.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Ieee Connect"
include(":app")
