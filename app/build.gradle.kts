plugins {
    id("com.android.application")
    kotlin("android")
}
android {
    namespace = "com.magnus.launcher"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.magnus.launcher"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // MQTT Paho
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    
    // Location (GPS)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
}

android {
    lint {
        baseline = file("lint-baseline.xml")
    }
}
