plugins {
    // Apply Kotlin plugin to all subprojects without applying it to the root project
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    // Common configuration for all subprojects can go here
} 