plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tsanetapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tsanetapp"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation ("org.pytorch:pytorch_android:1.13.1")  // Still latest stable as of early 2025
    implementation ("org.pytorch:pytorch_android_torchvision:1.13.1") // Optional
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}