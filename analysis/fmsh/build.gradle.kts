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
    applicationName = "fmsh"
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

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "fmsh",
            "Implementation-Version" to project.version
        )
    }
}

distributions {
    main {
        contents {
            from(rootProject.file("LICENSE"))
            from(rootProject.file("analysis/CHANGELOG.md"))
            from(projectDir.resolve("src/dist")) {
                into("")
            }
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}

tasks.distZip {
    enabled = false
}

tasks.distTar {
    enabled = true
}
