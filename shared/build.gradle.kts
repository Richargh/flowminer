plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("esm-distribution")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
                showStandardStreams = true
            }
        }
    }

    js(IR) {
        useEsModules()
        compilerOptions {
            moduleName.set("flowminer-shared")
        }
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":analysis:model"))
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions)
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter)
                implementation(libs.kotest.assertions)
            }
        }
        val jsMain by getting
        val jsTest by getting
    }
}

tasks.register<JavaExec>("generateSampleData") {
    description = "Generates sample visualization data"
    group = "application"
    mainClass.set("de.richargh.teamcharta.shared.GenerateSampleDataKt")
    val jvmCompilation = kotlin.jvm().compilations["main"]
    classpath = files(
        jvmCompilation.output.allOutputs,
        jvmCompilation.runtimeDependencyFiles
    )
    dependsOn("jvmMainClasses")
    args = listOf("${rootProject.projectDir}/visualization/src/assets/data.json")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    })
}

val javaToolchains = extensions.getByType<JavaToolchainService>()
