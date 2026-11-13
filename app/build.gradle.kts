plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "net.l2meta.scanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.l2meta.scanner"
        minSdk = 30          // takeScreenshot() з AccessibilityService з'явився в Android 11
        targetSdk = 35
        versionCode = 2
        versionName = "0.2"
    }

    // Постійний ключ підпису: кожна нова збірка ставиться поверх старої без видалення.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug { }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
