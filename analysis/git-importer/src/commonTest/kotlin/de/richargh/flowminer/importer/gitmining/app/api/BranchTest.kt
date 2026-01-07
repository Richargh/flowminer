package de.richargh.flowminer.importer.gitmining.app.api

import de.richargh.flowminer.importer.gitminingfixtures.app.api.aBranch
import de.richargh.flowminer.importer.sharedfixtures.time.app.atStartOfYear
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class BranchTest {

    @Test
    fun `age should be zero when firstCommit equals lastCommit`() {
        // When
        val branch = aBranch {
            firstCommitDate(atStartOfYear(2024))
            lastCommitDate(atStartOfYear(2024))
        }

        // Then
        branch.age shouldBe Duration.ZERO
    }

    @Test
    fun `age should be difference between lastCommit and firstCommit`() {
        // When
        val branch = aBranch {
            firstCommitDate(atStartOfYear(2024))
            lastCommitDate(atStartOfYear(2024) + 5.days)
        }

        // Then
        branch.age shouldBe 5.days
    }
}
