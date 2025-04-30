plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.map.secret)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    sourceSets {
        getByName("main") {
            java.srcDirs(
                "src/main/java",    // your existing Java sources
                "src/main/kotlin"   // your Kotlin-only files
            )
        }
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ← Your Maps key
        buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyBGSNMLZbBnhadJLSdKoIB67epDZxlwgiA\"")
        // ← Add your Google Weather key here
        buildConfigField("String", "WEATHER_API_KEY", "\"AIzaSyBGSNMLZbBnhadJLSdKoIB67epDZxlwgiA\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // 1) Compose BOM for unified versions (includes Material3 1.1.x)
    implementation(platform("androidx.compose:compose-bom:2024.03.00"))

    // 2) Core & Compose UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.material3:material3")
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.accompanist.permissions)

    // 3) Navigation & Architecture
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // 4) Firebase Auth & Firestore
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)

    // 5) Google Maps & Places
    implementation(libs.google.maps)
    implementation(libs.maps.compose)
    implementation(libs.places)

    // 6) Networking (OkHttp + Logging)
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation(libs.logging.interceptor)

    // 7) Android Material Components (provides cornerFamily & cornerSize)
    implementation("com.google.android.material:material:1.9.0")

    // 8) Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.03.00"))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

