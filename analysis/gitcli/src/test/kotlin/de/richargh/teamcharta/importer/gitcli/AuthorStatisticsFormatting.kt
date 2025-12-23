package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.git.app.api.Author
import de.richargh.teamcharta.importer.git.app.api.CommitType
import de.richargh.teamcharta.importer.gitminingfixtures.app.api.aAuthorStatistic
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Nested
class AuthorStatisticsFormatting {

    @Test
    fun `shows Author Statistics header with count`() {
        // When
        val authorStat = aAuthorStatistic {
            author("Alice", "alice@example.com")
            commitCount(5)
            linesAdded(100)
            linesRemoved(20)
        }

        val result = TableFormatter.formatAuthorStatistics(listOf(authorStat))

        // Then
        result shouldContain "Authors (1 out of 1)"
    }

    @Test
    fun `shows table with all columns`() {
        // When
        val authorStat = aAuthorStatistic {
            author("Alice Smith", "alice@example.com")
            commitCount(10)
            linesAdded(500)
            linesRemoved(150)
        }

        val result = TableFormatter.formatAuthorStatistics(listOf(authorStat))

        // Then
        result shouldContain "| Author"
        result shouldContain "| Commits"
        result shouldContain "| +Lines"
        result shouldContain "| -Lines"
        result shouldContain "| Alice Smith"
        result shouldContain "| 10"
        result shouldContain "| 500"
        result shouldContain "| 150"
    }

    @Test
    fun `sorts by commitCount descending`() {
        // When
        val alice = aAuthorStatistic {
            author("Alice", "alice@example.com")
            commitCount(5)
        }
        val bob = aAuthorStatistic {
            author("Bob", "bob@example.com")
            commitCount(15)
        }
        val charlie = aAuthorStatistic {
            author("Charlie", "charlie@example.com")
            commitCount(10)
        }

        val result = TableFormatter.formatAuthorStatistics(listOf(alice, bob, charlie))

        // Then - Bob (15) should come before Charlie (10) before Alice (5)
        val bobIndex = result.indexOf("Bob")
        val charlieIndex = result.indexOf("Charlie")
        val aliceIndex = result.indexOf("Alice")
        (bobIndex < charlieIndex) shouldBe true
        (charlieIndex < aliceIndex) shouldBe true
    }

    @Test
    fun `shows work items count column`() {
        // When
        val authorStat = aAuthorStatistic {
            author("Alice", "alice@example.com")
            commitCount(5)
            workItems("ABC-123", "XYZ-456", "DEF-789")
        }

        val result = TableFormatter.formatAuthorStatistics(listOf(authorStat))

        // Then
        result shouldContain "| WorkItems"
        result shouldContain "| 3"
    }

    @Test
    fun `shows churn by commit type columns`() {
        // When
        val authorStat = aAuthorStatistic {
            author("Alice", "alice@example.com")
            commitCount(5)
            churn(CommitType.FEATURE, 100, 20)
            churn(CommitType.FIX, 30, 10)
            churn(CommitType.REFACTOR, 50, 50)
        }

        val result = TableFormatter.formatAuthorStatistics(listOf(authorStat))

        // Then - columns F, B, R, T, D, E for each commit type
        result shouldContain "| F"
        result shouldContain "| B"
        result shouldContain "| R"
        result shouldContain "| 120" // FEATURE: 100+20
        result shouldContain "| 40"  // FIX: 30+10
        result shouldContain "| 100" // REFACTOR: 50+50
    }

    @Test
    fun `shows collaborators count column`() {
        // When
        val bob = Author("Bob", "bob@example.com")
        val charlie = Author("Charlie", "charlie@example.com")
        val authorStat = aAuthorStatistic {
            author("Alice", "alice@example.com")
            commitCount(5)
            collaborators(bob, charlie)
        }

        val result = TableFormatter.formatAuthorStatistics(listOf(authorStat))

        // Then
        result shouldContain "| Collabs"
        result shouldContain "| 2"
    }
}