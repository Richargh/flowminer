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
    implementation(project(":analysis:github-importer"))
    implementation(project(":analysis:model"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
}

// Add github-importer test classes to the test compile and runtime classpath
// This is needed because github-importer is a KMP project and test fixtures need special handling
val githubImporterTestClasses = files(
    project(":analysis:github-importer").layout.buildDirectory.dir("classes/kotlin/jvm/test")
)

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
    dependsOn(":analysis:github-importer:compileTestKotlinJvm")
    libraries.from(githubImporterTestClasses)
}

tasks.named<Test>("test") {
    classpath += githubImporterTestClasses
}

application {
    mainClass.set("de.richargh.teamcharta.importer.githubcli.GitHubCliKt")
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
