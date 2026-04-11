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
    implementation(project(":analysis:model"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Add git-importer test classes to the test compile and runtime classpath
// This is needed because git-importer is a KMP project and test fixtures need special handling
val gitImporterTestClasses = files(
    project(":analysis:git-importer").layout.buildDirectory.dir("classes/kotlin/jvm/test")
)

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
    dependsOn(":analysis:git-importer:compileTestKotlinJvm")
    libraries.from(gitImporterTestClasses)
}

tasks.named<Test>("test") {
    classpath += gitImporterTestClasses
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
