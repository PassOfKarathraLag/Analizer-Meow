plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.analizermeow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.analizermeow"
        minSdk = 26          // Android 8.0 (requerido para getScanResults sin location en algunos casos)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-premium"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Gráficas (canales, ruido, velocidad)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Corrutinas para tareas en segundo plano (scan, ping, speed test)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Lifecycle / ViewModel para mantener datos al rotar pantalla
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.1")
}
