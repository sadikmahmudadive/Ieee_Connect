plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.example.ieeeconnect"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ieeeconnect"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
        // Disable schema verification to avoid native library issues on Windows
        arg("room.verifySchema", "false")
    }
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
    kapt(libs.glide.compiler)
    // annotationProcessor(libs.glide.compiler) // Use kapt for Kotlin

    // Room
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)

    // sqlite-jdbc for kapt (Room verifier on Windows)
    kapt(libs.sqlite.jdbc)

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