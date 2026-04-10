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
    implementation(project(":analysis:gitlab-importer"))
    implementation(project(":analysis:model"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockwebserver)
}

// Add gitlab-importer test classes to the test compile and runtime classpath
val gitlabImporterTestClasses = files(
    project(":analysis:gitlab-importer").layout.buildDirectory.dir("classes/kotlin/jvm/test")
)

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
    dependsOn(":analysis:gitlab-importer:compileTestKotlinJvm")
    libraries.from(gitlabImporterTestClasses)
}

tasks.named<Test>("test") {
    classpath += gitlabImporterTestClasses
}

application {
    mainClass.set("de.richargh.flowminer.importer.gitlabcli.GitLabCliKt")
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
