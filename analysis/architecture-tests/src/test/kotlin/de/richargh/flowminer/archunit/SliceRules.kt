package de.richargh.flowminer.archunit

import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.CompositeArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

object SliceRules {

    fun internalOnlyAccessibleFromEntrypoint(basePackage: String): ArchRule =
        classes()
            .that().resideInAnyPackage("$basePackage.app.internal", "$basePackage.app.internal..")
            .should().onlyHaveDependentClassesThat()
            .resideInAnyPackage(
                "$basePackage.app",
                "$basePackage.app.internal",
                "$basePackage.app.internal.."
            )
            .because("internal packages should only be accessed by the slice entrypoint")

    fun apiShouldNotAccessInternal(basePackage: String): ArchRule =
        noClasses()
            .that().resideInAnyPackage("$basePackage.app.api", "$basePackage.app.api..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("$basePackage.app.internal", "$basePackage.app.internal..")
            .because("api packages should not depend on internal implementation")

    fun sliceArchitectureRules(basePackage: String): ArchRule =
        CompositeArchRule.of(
            internalOnlyAccessibleFromEntrypoint(basePackage)
        ).and(
            apiShouldNotAccessInternal(basePackage)
        )
}
