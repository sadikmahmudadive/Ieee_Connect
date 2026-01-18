plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.ieeeconnect"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ieeeconnect"
        minSdk = 26
        targetSdk = 35 
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

    // Explicitly force versions compatible with compileSdk 35 to resolve AAR conflicts
    configurations.all {
        resolutionStrategy {
            force("androidx.activity:activity:1.9.3")
            force("androidx.activity:activity-ktx:1.9.3")
            force("androidx.navigation:navigation-fragment:2.8.5")
            force("androidx.navigation:navigation-ui:2.8.5")
            force("androidx.navigation:navigation-common:2.8.5")
            force("androidx.navigation:navigation-runtime:2.8.5")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    implementation(libs.swiperefreshlayout)
    implementation(libs.gridlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.storage)

    implementation(libs.work.runtime)
    implementation(libs.cloudinary.android)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    ksp(libs.sqlite.jdbc)

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.github.yalantis:ucrop:2.2.8")

    implementation(libs.lottie)
    implementation(libs.shimmer)
    implementation(libs.circleimageview)
    implementation(libs.agora)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
