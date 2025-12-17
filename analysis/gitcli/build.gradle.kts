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
    implementation(project(":analysis:git-importer"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testImplementation(testFixtures(project(":analysis:git-importer")))
}

application {
    mainClass.set("de.richargh.teamcharta.importer.gitcli.GitCliKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = libs.versions.java.get()
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
