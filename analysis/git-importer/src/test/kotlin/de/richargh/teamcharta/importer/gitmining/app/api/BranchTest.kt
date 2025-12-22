package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.shared.time.app.atStartOfYear
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
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
