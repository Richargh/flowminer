plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// TODO make KMP-native
// Create a jar of JVM test classes for sharing test fixtures with other projects
val jvmTestJar by tasks.registering(Jar::class) {
    archiveClassifier.set("jvm-test")
    from(kotlin.jvm().compilations["test"].output.allOutputs)
}

// Create a configuration for consuming the test jar
val jvmTestElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
    }
}

artifacts {
    add("jvmTestElements", jvmTestJar)
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
