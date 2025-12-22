plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.application)
    `java-test-fixtures`
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
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testFixturesImplementation(libs.kotest.assertions)
}

application {
    mainClass.set("de.richargh.teamcharta.importer.git.GitLogParserCliKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
}
