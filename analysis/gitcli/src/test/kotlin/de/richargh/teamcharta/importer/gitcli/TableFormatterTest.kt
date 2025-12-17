package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.git.app.api.CommitType
import de.richargh.teamcharta.importer.git.app.api.FileChange
import de.richargh.teamcharta.importer.git.app.api.WorkKey
import de.richargh.teamcharta.importer.git.app.api.aCommit
import de.richargh.teamcharta.importer.gitmining.app.api.aBranch
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TableFormatterTest {

    private val now = ZonedDateTime.parse("2024-01-20T10:00:00+01:00")

    @Nested
    inner class ActiveBranches {

        @Test
        fun `shows Active Branches header with count`() {
            // When
            val branch = aBranch {
                name("feature-login")
                isActive()
                lastCommitDate(now.minusDays(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "Active Branches (1 out of 1)"
        }

        @Test
        fun `shows Active Branches header with multiple branches`() {
            // When
            val branches = (1..5).map { i ->
                aBranch {
                    name("branch-$i")
                    isActive()
                    lastCommitDate(now.minusDays(i.toLong()))
                }
            }

            val result = TableFormatter.formatBranches(branches, now)

            // Then
            result shouldContain "Active Branches (5 out of 5)"
        }

        @Test
        fun `marks current branch with asterisk`() {
            // When
            val branch = aBranch {
                name("feature-login")
                isActive()
                isCurrent()
                lastCommitDate(now.minusDays(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "* feature-login"
        }

        @Test
        fun `does not mark non-current branch with asterisk`() {
            // When
            val branch = aBranch {
                name("feature-login")
                isActive()
                lastCommitDate(now.minusDays(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "  feature-login"
            result shouldNotContain "* feature-login"
        }

        @Test
        fun `sorts by lastCommitDate newest first`() {
            // When
            val older = aBranch {
                name("older-branch")
                isActive()
                lastCommitDate(now.minusDays(5))
            }
            val newer = aBranch {
                name("newer-branch")
                isActive()
                lastCommitDate(now.minusDays(1))
            }

            val result = TableFormatter.formatBranches(listOf(older, newer), now)

            // Then
            val newerIndex = result.indexOf("newer-branch")
            val olderIndex = result.indexOf("older-branch")
            (newerIndex < olderIndex) shouldBe true
        }

        @Test
        fun `shows all active branches without limit`() {
            // When
            val branches = (1..15).map { i ->
                aBranch {
                    name("branch-$i")
                    isActive()
                    lastCommitDate(now.minusDays(i.toLong()))
                }
            }

            val result = TableFormatter.formatBranches(branches, now)

            // Then
            (1..15).forEach { i ->
                result shouldContain "branch-$i"
            }
        }
    }

    @Nested
    inner class StaleBranches {

        @Test
        fun `shows Stale Branches header with count`() {
            // When
            val branch = aBranch {
                name("stale-feature")
                isStale()
                lastCommitDate(now.minusMonths(3))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "Stale Branches (1 out of 1)"
        }

        @Test
        fun `shows Stale Branches header with limited count`() {
            // When
            val branches = (1..15).map { i ->
                aBranch {
                    name("stale-$i")
                    isStale()
                    lastCommitDate(now.minusMonths(i.toLong()))
                }
            }

            val result = TableFormatter.formatBranches(branches, now)

            // Then
            result shouldContain "Stale Branches (10 out of 15)"
        }

        @Test
        fun `sorts by lastCommitDate oldest first`() {
            // When
            val older = aBranch {
                name("oldest-stale")
                isStale()
                lastCommitDate(now.minusMonths(6))
            }
            val newer = aBranch {
                name("newer-stale")
                isStale()
                lastCommitDate(now.minusMonths(3))
            }

            val result = TableFormatter.formatBranches(listOf(newer, older), now)

            // Then
            val olderIndex = result.indexOf("oldest-stale")
            val newerIndex = result.indexOf("newer-stale")
            (olderIndex < newerIndex) shouldBe true
        }

        @Test
        fun `limits to 10 entries and shows count of hidden`() {
            // When
            val branches = (1..15).map { i ->
                aBranch {
                    name("stale-$i")
                    isStale()
                    lastCommitDate(now.minusMonths(i.toLong()))
                }
            }

            val result = TableFormatter.formatBranches(branches, now)

            // Then
            result shouldContain "5 more stale"
        }
    }

    @Nested
    inner class CompletedBranches {

        @Test
        fun `shows Completed Branches header with count`() {
            // When
            val branch = aBranch {
                name("completed-feature")
                isCompleted()
                lastCommitDate(now.minusWeeks(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "Completed Branches (1 out of 1)"
        }

        @Test
        fun `shows Completed Branches header with limited count`() {
            // When
            val branches = (1..12).map { i ->
                aBranch {
                    name("completed-$i")
                    isCompleted()
                    lastCommitDate(now.minusWeeks(i.toLong()))
                }
            }

            val result = TableFormatter.formatBranches(branches, now)

            // Then
            result shouldContain "Completed Branches (10 out of 12)"
        }

        @Test
        fun `sorts by lastCommitDate newest first`() {
            // When
            val older = aBranch {
                name("older-completed")
                isCompleted()
                lastCommitDate(now.minusMonths(2))
            }
            val newer = aBranch {
                name("newer-completed")
                isCompleted()
                lastCommitDate(now.minusWeeks(1))
            }

            val result = TableFormatter.formatBranches(listOf(older, newer), now)

            // Then
            val newerIndex = result.indexOf("newer-completed")
            val olderIndex = result.indexOf("older-completed")
            (newerIndex < olderIndex) shouldBe true
        }

        @Test
        fun `limits to 10 entries and shows count of hidden`() {
            // When
            val branches = (1..12).map { i ->
                aBranch {
                    name("completed-$i")
                    isCompleted()
                    lastCommitDate(now.minusWeeks(i.toLong()))
                }
            }

            val result = TableFormatter.formatBranches(branches, now)

            // Then
            result shouldContain "2 more completed"
        }
    }

    @Nested
    inner class RelativeDateFormatting {

        @Test
        fun `shows days ago for recent commits`() {
            // When
            val branch = aBranch {
                name("feature")
                isActive()
                lastCommitDate(now.minusDays(3))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "3 days ago"
        }

        @Test
        fun `shows weeks ago for commits older than a week`() {
            // When
            val branch = aBranch {
                name("feature")
                isActive()
                lastCommitDate(now.minusWeeks(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "2 weeks ago"
        }

        @Test
        fun `shows months ago for commits older than a month`() {
            // When
            val branch = aBranch {
                name("feature")
                isStale()
                lastCommitDate(now.minusMonths(3))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "3 months ago"
        }
    }

    @Nested
    inner class AgeFormatting {

        @Test
        fun `shows age as period from firstCommit to now in days`() {
            // When
            val branch = aBranch {
                name("feature")
                isActive()
                firstCommitDate(now.minusDays(5))
                lastCommitDate(now.minusDays(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "5 days"
        }

        @Test
        fun `shows age as period from firstCommit to now in weeks`() {
            // When
            val branch = aBranch {
                name("feature")
                isActive()
                firstCommitDate(now.minusWeeks(3))
                lastCommitDate(now.minusDays(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "3 weeks"
        }

        @Test
        fun `shows age as period from firstCommit to now in months`() {
            // When
            val branch = aBranch {
                name("feature")
                isStale()
                firstCommitDate(now.minusMonths(4))
                lastCommitDate(now.minusMonths(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "4 months"
        }
    }

    @Nested
    inner class TableColumns {

        @Test
        fun `shows Name Updated and Age columns`() {
            // When
            val branch = aBranch {
                name("feature")
                isActive()
                firstCommitDate(now.minusDays(5))
                lastCommitDate(now.minusDays(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "Name"
            result shouldContain "Updated"
            result shouldContain "Age"
        }
    }

    @Nested
    inner class EmptySections {

        @Test
        fun `shows No Active Branches when no active branches exist`() {
            // When
            val branch = aBranch {
                name("stale-feature")
                isStale()
                lastCommitDate(now.minusMonths(3))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "No Active Branches"
        }

        @Test
        fun `shows No Stale Branches when no stale branches exist`() {
            // When
            val branch = aBranch {
                name("active-feature")
                isActive()
                lastCommitDate(now.minusDays(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "No Stale Branches"
        }

        @Test
        fun `shows No Completed Branches when no completed branches exist`() {
            // When
            val branch = aBranch {
                name("active-feature")
                isActive()
                lastCommitDate(now.minusDays(2))
            }

            val result = TableFormatter.formatBranches(listOf(branch), now)

            // Then
            result shouldContain "No Completed Branches"
        }
    }

    @Nested
    inner class CommitFormatting {

        @Test
        fun `shows Branch column with branch name`() {
            // When
            val commit = aCommit {
                certainBranch("main")
                date(ZonedDateTime.parse("2024-01-15T10:30:00+01:00"))
                author("Jane Doe", "jane@example.com")
                message("Add login feature")
            }

            val result = TableFormatter.formatCommits(listOf(commit))

            // Then
            result shouldContain "| Branch"
            result shouldContain "|   main"
        }

        @Test
        fun `shows CommitType column`() {
            // When
            val commit = aCommit {
                commitType(CommitType.FEATURE)
                date(ZonedDateTime.parse("2024-01-15T10:30:00+01:00"))
                author("Jane Doe", "jane@example.com")
                message("Add login feature")
            }

            val result = TableFormatter.formatCommits(listOf(commit))

            // Then
            result shouldContain "| Type"
            result shouldContain "| FEATURE"
        }

        @Test
        fun `shows Workkeys column`() {
            // When
            val commit = aCommit {
                workKeys(WorkKey("ABC-123"), WorkKey("DEF-456"))
                date(ZonedDateTime.parse("2024-01-15T10:30:00+01:00"))
                author("Jane Doe", "jane@example.com")
                message("Add login feature")
            }

            val result = TableFormatter.formatCommits(listOf(commit))

            // Then
            result shouldContain "| Workkeys"
            result shouldContain "ABC-123,DEF-456"
        }

        @Test
        fun `shows Commits header with count`() {
            // When
            val commits = listOf(
                aCommit { message("First") },
                aCommit { message("Second") }
            )

            val result = TableFormatter.formatCommits(commits, totalCount = 5)

            // Then
            result shouldContain "Commits (2 out of 5)"
        }

        @Test
        fun `marks only the top commit on current branch with asterisk`() {
            // When
            val commits = listOf(
                aCommit {
                    certainBranch("main")
                    isOnCurrentBranch()
                    date(ZonedDateTime.parse("2024-01-15T10:30:00+01:00"))
                    message("HEAD commit")
                },
                aCommit {
                    certainBranch("main")
                    isOnCurrentBranch()
                    date(ZonedDateTime.parse("2024-01-14T10:30:00+01:00"))
                    message("Second commit on current branch")
                },
                aCommit {
                    certainBranch("feature")
                    date(ZonedDateTime.parse("2024-01-13T10:30:00+01:00"))
                    message("Other branch commit")
                }
            )

            val result = TableFormatter.formatCommits(commits)

            // Then
            val lines = result.lines()
            val dataLines = lines.filter { it.startsWith("|") && !it.contains("Branch") && !it.contains("---") }
            dataLines[0] shouldContain "| * main"
            dataLines[1] shouldContain "|   main"
            dataLines[2] shouldContain "|   feature"
        }

        @Test
        fun `shows total additions and deletions columns`() {
            // When
            val commit = aCommit {
                fileChanges(
                    FileChange("file1.kt", additions = 10, deletions = 5),
                    FileChange("file2.kt", additions = 20, deletions = 3)
                )
                date(ZonedDateTime.parse("2024-01-15T10:30:00+01:00"))
                author("Jane Doe", "jane@example.com")
                message("Add login feature")
            }

            val result = TableFormatter.formatCommits(listOf(commit))

            // Then
            result shouldContain "| +"
            result shouldContain "| -"
            result shouldContain "| 30"
            result shouldContain "| 8"
        }

        @Test
        fun `formats single commit as table with aligned columns`() {
            // When
            val commit = aCommit {
                certainBranch("main")
                date(ZonedDateTime.parse("2024-01-15T10:30:00+01:00"))
                commitType(CommitType.FEATURE)
                author("Jane Doe", "jane@example.com")
                workKeys(WorkKey("ABC-1"))
                fileChanges(FileChange("file.kt", additions = 10, deletions = 5))
                message("Add login feature")
            }

            val result = TableFormatter.formatCommits(listOf(commit))

            // Then
            result shouldBe """
                Commits (1 out of 1)
                | Branch | Date       | Type    | Author   | Workkeys | +  | - | Message           |
                |--------|------------|---------|----------|----------|----|---|-------------------|
                |   main | 2024-01-15 | FEATURE | Jane Doe | ABC-1    | 10 | 5 | Add login feature |
            """.trimIndent()
        }

        @Test
        fun `aligns columns based on longest value`() {
            // When
            val commits = listOf(
                aCommit {
                    certainBranch("main")
                    date(ZonedDateTime.parse("2024-01-15T10:30:00+01:00"))
                    commitType(CommitType.FIX)
                    author("Jane Doe", "jane@example.com")
                    message("Fix bug")
                },
                aCommit {
                    certainBranch("feature/auth")
                    date(ZonedDateTime.parse("2024-01-16T11:00:00+01:00"))
                    commitType(CommitType.FEATURE)
                    author("John Smith", "john@example.com")
                    message("Add authentication feature")
                }
            )

            val result = TableFormatter.formatCommits(commits)

            // Then
            result shouldBe """
                Commits (2 out of 2)
                | Branch         | Date       | Type    | Author     | Workkeys | + | - | Message                    |
                |----------------|------------|---------|------------|----------|---|---|----------------------------|
                |   main         | 2024-01-15 | FIX     | Jane Doe   |          | 0 | 0 | Fix bug                    |
                |   feature/auth | 2024-01-16 | FEATURE | John Smith |          | 0 | 0 | Add authentication feature |
            """.trimIndent()
        }
    }
}
