package de.richargh.teamcharta.importer.gitmining.app


import de.richargh.teamcharta.importer.git.app.api.Author
import de.richargh.teamcharta.importer.git.app.api.CommitType
import de.richargh.teamcharta.importer.git.app.api.WorkKey
import de.richargh.teamcharta.importer.gitfixtures.app.aGitLog
import de.richargh.teamcharta.importer.gitmining.app.api.AuthorContribution
import de.richargh.teamcharta.importer.sharedfixtures.time.app.testNow2025
import de.richargh.teamcharta.importer.sharedfixtures.time.app.toInstant
import de.richargh.teamcharta.importer.gitmining.app.api.FilePath
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import parse

class WorkItemDetectionTest {

    @Test
    fun `commits without WorkKey should be grouped as no-workitem`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                subject("Initial commit")
                file("src/Main.kt", additions = 5, deletions = 2)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.workItems.size() shouldBe 1
        // TODO should use unnamed
        val workItem = result.workItems.all().first()
        workItem.workKey shouldBe WorkKey.Unknown
        workItem.linesAdded shouldBe 5
        workItem.linesRemoved shouldBe 2
    }

    @Test
    fun `two commits with same WorkKey should result in one WorkItem with summed lines`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/feature") {
                subject("ABC-123 first commit")
                file("src/Feature.kt", additions = 10, deletions = 3)
            }
            anEntry("origin/feature") {
                subject("ABC-123 second commit")
                file("src/Feature.kt", additions = 5, deletions = 2)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.workItems.size() shouldBe 1
        val workItem = result.workItems["ABC-123"]!!
        workItem.workKey shouldBe WorkKey.Known("ABC-123")
        workItem.linesAdded shouldBe 15
        workItem.linesRemoved shouldBe 5
    }

    @Test
    fun `WorkItem should track unique files changed across commits`() {
        // Given - Two commits touching overlapping files
        val gitLogContent = aGitLog {
            anEntry("origin/feature") {
                subject("ABC-123 first commit")
                authorDate("2024-01-10T10:00:00+01:00".toInstant())
                file("src/Feature.kt", additions = 10, deletions = 0)
                file("src/Helper.kt", additions = 5, deletions = 0)
            }
            anEntry("origin/feature") {
                subject("ABC-123 second commit")
                authorDate("2024-01-11T10:00:00+01:00".toInstant())
                file("src/Feature.kt", additions = 3, deletions = 0)
                file("src/Service.kt", additions = 2, deletions = 0)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        val workItem = result.workItems["ABC-123"]!!
        workItem.filesChanged shouldContainExactlyInAnyOrder setOf(
            FilePath.of("src/Feature.kt"),
            FilePath.of("src/Helper.kt"),
            FilePath.of("src/Service.kt")
        )
    }

    @Test
    fun `WorkItem should track author contributions sorted by lines changed descending`() {
        // Given - Two authors with different contribution sizes
        val gitLogContent = aGitLog {
            anEntry("origin/feature") {
                subject("ABC-123 small change")
                author("Alice", "alice@example.com")
                file("src/Feature.kt", additions = 5, deletions = 2)
            }
            anEntry("origin/feature") {
                subject("ABC-123 big change")
                author("Bob", "bob@example.com")
                file("src/Service.kt", additions = 50, deletions = 10)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then - Contributions sorted by total lines (additions + deletions) descending
        val workItem = result.workItems["ABC-123"]!!
        workItem.contributions.size shouldBe 2
        workItem.contributions shouldContainExactly listOf(
            AuthorContribution(Author("Bob", "bob@example.com"), linesChanged = 60),
            AuthorContribution(Author("Alice", "alice@example.com"), linesChanged = 7)
        )
    }

    @Test
    fun `WorkItem should track lines changed by commit type`() {
        // Given - Commits with different types
        val gitLogContent = aGitLog {
            anEntry("origin/feature") {
                subject("feat: ABC-123 add new feature")
                file("src/Feature.kt", additions = 20, deletions = 5)
            }
            anEntry("origin/feature") {
                subject("fix: ABC-123 fix bug")
                file("src/BugFix.kt", additions = 10, deletions = 3)
            }
            anEntry("origin/feature") {
                subject("test: ABC-123 add tests")
                file("src/FeatureTest.kt", additions = 30, deletions = 0)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then - Lines are aggregated by commit type
        val workItem = result.workItems["ABC-123"]!!
        workItem.absoluteChurnByType shouldContainExactly mapOf(
            CommitType.FEATURE to 25,  // 20 + 5
            CommitType.FIX to 13,      // 10 + 3
            CommitType.TEST to 30      // 30 + 0
        )
    }

    @Test
    fun `WorkItem should track commit count`() {
        // Given - Three commits for same work item
        val gitLogContent = aGitLog {
            anEntry("origin/feature") {
                subject("ABC-123 first commit")
                file("src/Feature.kt", additions = 10, deletions = 0)
            }
            anEntry("origin/feature") {
                subject("ABC-123 second commit")
                file("src/Feature.kt", additions = 5, deletions = 0)
            }
            anEntry("origin/feature") {
                subject("ABC-123 third commit")
                file("src/Feature.kt", additions = 3, deletions = 0)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        val workItem = result.workItems["ABC-123"]!!
        workItem.commits shouldBe 3
    }

    @Test
    fun `WorkItem should track collaborator count including co-authors`() {
        // Given - Two commits with authors and co-authors
        val gitLogContent = aGitLog {
            anEntry("origin/feature") {
                subject("ABC-123 first commit")
                author("Alice", "alice@example.com")
                // Co-authors come from trailers
                trailers("Co-authored-by" to "Bob <bob@example.com>")
                file("src/Feature.kt", additions = 10, deletions = 0)
            }
            anEntry("origin/feature") {
                subject("ABC-123 second commit")
                author("Charlie", "charlie@example.com")
                file("src/Feature.kt", additions = 5, deletions = 0)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then - 3 distinct collaborators: Alice, Bob (co-author), Charlie
        val workItem = result.workItems["ABC-123"]!!
        workItem.collaborators shouldBe 3
    }

    @Test
    fun `WorkItem should track rework files - files modified in multiple commits`() {
        // Given - Three commits, some touching same files
        val gitLogContent = aGitLog {
            anEntry("origin/feature") {
                subject("ABC-123 first commit")
                file("src/Feature.kt", additions = 10, deletions = 0)
                file("src/Helper.kt", additions = 5, deletions = 0)
            }
            anEntry("origin/feature") {
                subject("ABC-123 second commit")
                file("src/Feature.kt", additions = 3, deletions = 0)  // rework
                file("src/Service.kt", additions = 2, deletions = 0)
            }
            anEntry("origin/feature") {
                subject("ABC-123 third commit")
                file("src/Feature.kt", additions = 1, deletions = 0)  // more rework
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then - Feature.kt touched 3 times = rework
        val workItem = result.workItems["ABC-123"]!!
        workItem.reworkFiles shouldContainExactlyInAnyOrder setOf(FilePath.of("src/Feature.kt"))
    }

    @Test
    fun `commit with multiple WorkKeys should contribute to all referenced work items`() {
        // Given - One commit referencing two work items
        val gitLogContent = aGitLog {
            anEntry("origin/feature") {
                subject("ABC-123 DEF-456 implement shared feature")
                file("src/Shared.kt", additions = 20, deletions = 5)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then - Both work items should exist with same lines
        result.workItems.size() shouldBe 2

        val workItem1 = result.workItems["ABC-123"]!!
        workItem1.linesAdded shouldBe 20
        workItem1.linesRemoved shouldBe 5
        workItem1.commits shouldBe 1

        val workItem2 = result.workItems["DEF-456"]!!
        workItem2.linesAdded shouldBe 20
        workItem2.linesRemoved shouldBe 5
        workItem2.commits shouldBe 1
    }
}
