package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.git.app.api.aCommit
import de.richargh.teamcharta.importer.gitmining.app.api.aBranch
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TableFormatterTest {

    @Test
    fun `formats single branch as table with aligned columns`() {
        val branch = aBranch {
            name("feature-login")
            firstCommitDate(ZonedDateTime.parse("2024-01-10T10:00:00+01:00"))
            lastCommitDate(ZonedDateTime.parse("2024-01-15T14:30:00+01:00"))
        }

        val result = TableFormatter.formatBranches(listOf(branch))

        result shouldBe """
            | Name          | First Commit | Last Commit | Merged |
            |---------------|--------------|-------------|--------|
            | feature-login | 2024-01-10   | 2024-01-15  | No     |
        """.trimIndent()
    }

    @Test
    fun `formats merged branch with Yes in merged column`() {
        val branch = aBranch {
            name("feature-auth")
            firstCommitDate(ZonedDateTime.parse("2024-01-10T10:00:00+01:00"))
            lastCommitDate(ZonedDateTime.parse("2024-01-12T14:30:00+01:00"))
            mergedInto("main", ZonedDateTime.parse("2024-01-13T10:00:00+01:00"), "merge123")
        }

        val result = TableFormatter.formatBranches(listOf(branch))

        result shouldBe """
            | Name         | First Commit | Last Commit | Merged |
            |--------------|--------------|-------------|--------|
            | feature-auth | 2024-01-10   | 2024-01-12  | Yes    |
        """.trimIndent()
    }

    @Test
    fun `formats single commit as table with aligned columns`() {
        val commit = aCommit {
            hash("abc123def")
            date(ZonedDateTime.parse("2024-01-15T10:30:00+01:00"))
            author("Jane Doe", "jane@example.com")
            message("Add login feature")
        }

        val result = TableFormatter.formatCommits(listOf(commit))

        result shouldBe """
            | Hash    | Date       | Author   | Message           |
            |---------|------------|----------|-------------------|
            | abc123d | 2024-01-15 | Jane Doe | Add login feature |
        """.trimIndent()
    }

    @Test
    fun `aligns columns based on longest value`() {
        val commits = listOf(
            aCommit {
                hash("abc123d")
                date(ZonedDateTime.parse("2024-01-15T10:30:00+01:00"))
                author("Jane Doe", "jane@example.com")
                message("Fix bug")
            },
            aCommit {
                hash("def456e")
                date(ZonedDateTime.parse("2024-01-16T11:00:00+01:00"))
                author("John Smith", "john@example.com")
                message("Add authentication feature")
            }
        )

        val result = TableFormatter.formatCommits(commits)

        result shouldBe """
            | Hash    | Date       | Author     | Message                    |
            |---------|------------|------------|----------------------------|
            | abc123d | 2024-01-15 | Jane Doe   | Fix bug                    |
            | def456e | 2024-01-16 | John Smith | Add authentication feature |
        """.trimIndent()
    }
}
