plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.2") // Or your current Gradle plugin version
        // Add other buildscript dependencies here if needed
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}