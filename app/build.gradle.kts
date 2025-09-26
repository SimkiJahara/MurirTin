plugins {
    // Apply plugins for Android app development, Kotlin, Jetpack Compose, and Firebase.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Android configuration for the মুড়ির টিন app.
android {
    // App namespace for resource access.
    namespace = "com.example.muritin"
    // Compile SDK version for Android 14.
    compileSdk = 34

    defaultConfig {
        // Unique app ID.
        applicationId = "com.example.muritin"
        // Minimum SDK version (Android 7.0 Nougat).
        minSdk = 24
        // Target SDK version (Android 14).
        targetSdk = 34
        // App version code for Play Store.
        versionCode = 1
        // App version name for display.
        versionName = "1.0"
        // Test runner for Android instrumentation tests.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Build types for debug and release.
    buildTypes {
        release {
            // Disable minification for simplicity in demo.
            isMinifyEnabled = false
            // Proguard rules for optimization (not used in demo).
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // Java/Kotlin compatibility set to Java 17.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // Enable Jetpack Compose for UI.
    buildFeatures {
        compose = true
    }
}

// Dependencies required for the app.
dependencies {
    // Core Android and Kotlin libraries.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Jetpack Compose libraries for UI.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    // Firebase libraries for authentication, database, and messaging.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.messaging.ktx)
    // Coroutines for Firebase async operations.
    implementation(libs.kotlinx.coroutines.play.services)
    // Google Play Services for maps and location (for future features).
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    // Additional Compose libraries for icons and view models.
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    // Test dependencies.
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Debug tools for Compose.
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}