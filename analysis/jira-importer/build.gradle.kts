plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.application)
    alias(libs.plugins.kotlin.kapt)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.picocli)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.jsr310)
    implementation(libs.okhttp)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    kapt(libs.moshi.codegen)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotest.assertions)
}

application {
    mainClass.set("de.richargh.teamcharta.importer.jira.JiraImporterKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
        vendor.set(org.gradle.jvm.toolchain.JvmVendorSpec.ADOPTIUM)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = libs.versions.java.get()
}

tasks.test {
    useJUnitPlatform()
} 