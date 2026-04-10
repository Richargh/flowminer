plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.testBalloon)
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
                events("skipped", "failed")
            }
        }
    }

    js(IR) {
        useEsModules()
        browser {
            webpackTask {
                mainOutputFileName = "flowminer-gitlab-importer.js"
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
                testLogging {
                    events("skipped", "failed")
                }
            }
        }
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            moduleName.set("flowminer-gitlab-importer")
            useEsClasses.set(true)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":analysis:model"))
                implementation(project(":shared"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions)
                implementation(libs.testBalloon.framework.core)
                implementation(libs.testBalloon.integration.kotest.assertions)
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}
