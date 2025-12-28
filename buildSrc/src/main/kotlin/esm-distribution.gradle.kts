import org.gradle.api.tasks.Copy

/**
 * Convention plugin for configuring ESM distribution for Kotlin/JS libraries.
 *
 * This plugin:
 * 1. Copies ESM (.mjs) files to the distribution folder
 * 2. Updates package.json to use "type": "module"
 */

afterEvaluate {
    // Task to copy ESM files to distribution folder
    tasks.register<Copy>("copyEsmToDistribution") {
        dependsOn("compileProductionLibraryKotlinJs")
        from("build/compileSync/js/main/productionLibrary/kotlin")
        into("build/dist/js/productionLibrary")
        include("*.mjs", "*.mjs.map", "*.d.ts")
    }

    // Task to update package.json for ESM
    tasks.register("updatePackageJsonForEsm") {
        dependsOn("copyEsmToDistribution")
        doLast {
            val packageJson = file("build/dist/js/productionLibrary/package.json")
            if (packageJson.exists()) {
                var content = packageJson.readText()
                // Add type: module after the opening brace
                if (!content.contains("\"type\": \"module\"")) {
                    content = content.replaceFirst("{", "{\n  \"type\": \"module\",")
                }
                packageJson.writeText(content)
            }
        }
    }

    // Make the distribution task finalize with ESM setup
    tasks.findByName("jsBrowserProductionLibraryDistribution")?.finalizedBy("updatePackageJsonForEsm")
}
