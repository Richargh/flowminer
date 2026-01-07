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
    implementation(project(":analysis:gitcli"))
    implementation(project(":analysis:github-cli"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
}

application {
    mainClass.set("de.richargh.flowminer.fmsh.FmshKt")
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
    testLogging {
        events("skipped", "failed")
    }
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
