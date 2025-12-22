plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
        binaries {
            executable {
                mainClass.set("de.richargh.teamcharta.importer.git.GitLogParserCliKt")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // No additional dependencies needed - using kotlin stdlib
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.picocli)
                implementation(libs.jackson.module.kotlin)
                implementation(libs.jackson.databind)
                implementation(libs.jackson.jsr310)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter)
            }
        }
    }
}
