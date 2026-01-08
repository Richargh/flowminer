package de.richargh.flowminer.archunit

import com.tngtech.archunit.core.importer.ClassFileImporter
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class SliceRulesTest {

    @Test
    fun `internal should only be accessible from entrypoint - valid case passes`() {
        val classes = ClassFileImporter()
            .importPackages("de.richargh.flowminer.archunit.testfixtures.valid")

        val rule = SliceRules.internalOnlyAccessibleFromEntrypoint(
            "de.richargh.flowminer.archunit.testfixtures.valid"
        )

        rule.check(classes)
    }

    @Test
    fun `internal should only be accessible from entrypoint - invalid case fails`() {
        val classes = ClassFileImporter()
            .importPackages("de.richargh.flowminer.archunit.testfixtures.invalid")

        val rule = SliceRules.internalOnlyAccessibleFromEntrypoint(
            "de.richargh.flowminer.archunit.testfixtures.invalid"
        )

        shouldThrow<AssertionError> {
            rule.check(classes)
        }
    }

    @Test
    fun `api should not access internal - valid case passes`() {
        val classes = ClassFileImporter()
            .importPackages("de.richargh.flowminer.archunit.testfixtures.apivalid")

        val rule = SliceRules.apiShouldNotAccessInternal(
            "de.richargh.flowminer.archunit.testfixtures.apivalid"
        )

        rule.check(classes)
    }

    @Test
    fun `api should not access internal - invalid case fails`() {
        val classes = ClassFileImporter()
            .importPackages("de.richargh.flowminer.archunit.testfixtures.apiinvalid")

        val rule = SliceRules.apiShouldNotAccessInternal(
            "de.richargh.flowminer.archunit.testfixtures.apiinvalid"
        )

        shouldThrow<AssertionError> {
            rule.check(classes)
        }
    }

    @Test
    fun `slice architecture rules combines all rules - valid case passes`() {
        val classes = ClassFileImporter()
            .importPackages("de.richargh.flowminer.archunit.testfixtures.valid")

        val rules = SliceRules.sliceArchitectureRules(
            "de.richargh.flowminer.archunit.testfixtures.valid"
        )

        rules.check(classes)
    }

    @Test
    fun `slice architecture rules combines all rules - internal violation fails`() {
        val classes = ClassFileImporter()
            .importPackages("de.richargh.flowminer.archunit.testfixtures.invalid")

        val rules = SliceRules.sliceArchitectureRules(
            "de.richargh.flowminer.archunit.testfixtures.invalid"
        )

        shouldThrow<AssertionError> {
            rules.check(classes)
        }
    }

    @Test
    fun `slice architecture rules combines all rules - api violation fails`() {
        val classes = ClassFileImporter()
            .importPackages("de.richargh.flowminer.archunit.testfixtures.apiinvalid")

        val rules = SliceRules.sliceArchitectureRules(
            "de.richargh.flowminer.archunit.testfixtures.apiinvalid"
        )

        shouldThrow<AssertionError> {
            rules.check(classes)
        }
    }
}
