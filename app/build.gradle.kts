plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    // Do NOT apply KSP here; the project did not include KSP plugin in the version catalog and the environment had trouble resolving it.
}

android {
    namespace = "com.example.ieeeconnect"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ieeeconnect"
        minSdk = 26
        targetSdk = 36 // Updated to the latest version
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Removed incorrect `java { toolchain { ... } }` from defaultConfig. Toolchains are configured at module or top-level, not inside defaultConfig.
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        // Use Java 17 toolchain for compatibility with the installed JDK on the developer machine
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Keep the same Kotlin jvm toolchain at module-level too (will be set below)
}

kotlin {
    // Use JDK17 toolchain for Kotlin compilation and kapt to avoid module illegal-access problems
    jvmToolchain(17)
}

dependencies {
    // Core Android UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    // SwipeRefreshLayout used by fragment_home.xml
    implementation(libs.swiperefreshlayout)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Firebase BoM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.auth)
    // Firebase Storage (for uploading banners)
    implementation(libs.firebase.storage)

    // WorkManager for background upload/queue
    implementation(libs.work.runtime)

    // Cloudinary
    implementation(libs.cloudinary.android)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Glide
    implementation(libs.glide)

    // Room
    implementation(libs.room.runtime) // Updated to 2.8.4
    // Use KAPT (not annotationProcessor) for Kotlin projects
    kapt(libs.room.compiler)

    // Add sqlite-jdbc to the KAPT classpath so Room's verifier can find a native library during annotation processing on Windows.
    // Use an older, widely-compatible version that contains the expected native layout for Windows x86_64.
    kapt("org.xerial:sqlite-jdbc:3.36.0.3")

    // Also add sqlite-jdbc to the runtime/implementation classpath so native resources can be found when annotation processors run.
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")

    // QR Code
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.4.1")

    // Image Cropping
    implementation("com.github.yalantis:ucrop:2.2.8")

    // Animations & polish
    implementation(libs.lottie)
    implementation(libs.shimmer)
    implementation(libs.circleimageview)

    // Agora
    implementation(libs.agora)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

kapt {
    correctErrorTypes = true
    // Avoid forcing the compile classpath into kapt; keep default false unless you have a reason
    includeCompileClasspath = false
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
        // Disable schema verification to avoid SQLite native error on Windows
        arg("room.verifySchema", "false")
    }
}
