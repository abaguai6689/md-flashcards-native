plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.suishibeisong.flashcards"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.suishibeisong.flashcards"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "1.6.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("fixed") {
            // Fixed sideloading key so every release can update the previous install.
            // NOTE: this key is public in the repository. Do NOT use it for Play Store.
            // Override via env on CI/private builds if needed.
            storeFile = rootProject.file("keystore/md-flashcards.jks")
            storePassword = System.getenv("MD_KEYSTORE_PASSWORD") ?: "mdflashcards2026"
            keyAlias = System.getenv("MD_KEY_ALIAS") ?: "mdflashcards"
            keyPassword = System.getenv("MD_KEY_PASSWORD") ?: "mdflashcards2026"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("dev.chrisbanes.haze:haze:1.2.2")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
