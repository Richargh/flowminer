package de.richargh.flowminer.archunit

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class SliceArchitectureTest {

    // Check the build.gradle.kts to see which projects are analyzed
    private val productionClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("de.richargh.flowminer")

    @Test
    fun `internal packages should only be accessed from slice entrypoint`() {
        val rule: ArchRule = classes()
            .that().resideInAPackage("..app.internal..")
            .should().onlyHaveDependentClassesThat()
            .resideInAnyPackage("..app", "..app.internal..")
            .because("internal packages should only be accessed by the slice entrypoint")

        rule.check(productionClasses)
    }

    @Test
    fun `api packages should not depend on internal`() {
        val rule: ArchRule = noClasses()
            .that().resideInAPackage("..app.api..")
            .should().dependOnClassesThat()
            .resideInAPackage("..app.internal..")
            .because("api packages should not depend on internal implementation")

        rule.check(productionClasses)
    }
}
