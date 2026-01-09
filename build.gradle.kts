// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // NOTE: The `org.gradle.toolchains.foojay-resolver` plugin MUST be applied in the SETTINGS script (settings.gradle.kts)
    // not in the top-level build script. Applying it here causes Gradle to throw:
    // "The plugin must be applied in a settings script (or to the Settings object), but was applied in a build script".
    // If you want automatic Java toolchain downloads, add this to `settings.gradle.kts` instead:
    // plugins { id("org.gradle.toolchains.foojay-resolver") version "0.4.0" }

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

// Ensure this file only contains top-level configurations. Move dependencies to module-level build.gradle.kts files.

// NOTE:
// If you need to configure Java toolchains for modules, do that per-module (or via a subprojects block)
// e.g. in a module's build.gradle.kts use:
// java {
//   toolchain { languageVersion.set(JavaLanguageVersion.of(11)) }
// }
