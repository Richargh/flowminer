plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.application)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.picocli)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.databind)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotest.assertions)
}

application {
    mainClass.set("JiraExtractorKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
        vendor.set(org.gradle.jvm.toolchain.JvmVendorSpec.ADOPTIUM)
    }
}

application {
    mainClass.set("JiraExtractorKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = libs.versions.java.get()
}

tasks.test {
    useJUnitPlatform()
} 