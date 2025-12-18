package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.aGitLog
import de.richargh.teamcharta.importer.git.app.api.Author
import de.richargh.teamcharta.importer.git.app.api.CommitType
import de.richargh.teamcharta.importer.git.app.api.WorkKey
import de.richargh.teamcharta.importer.gitmining.app.api.ChurnMetric
import de.richargh.teamcharta.importer.shared.time.app.testNow2025
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AuthorStatisticsDetectionTest {

    @Test
    fun `single author with single commit should have commitCount of 1`() {
        // Given
        val alice = Author("Alice", "alice@example.com")
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                author(alice)
                file("src/Main.kt", additions = 5, deletions = 2)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.authorStatistics.size() shouldBe 1
        result.authorStatistics[alice]!!.commitCount shouldBe 1
    }

    @Test
    fun `single author with multiple commits should aggregate commitCount`() {
        // Given
        val alice = Author("Alice", "alice@example.com")
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                author(alice)
                file("src/Main.kt", additions = 5, deletions = 0)
            }
            anEntry("origin/main") {
                author(alice)
                file("src/Helper.kt", additions = 10, deletions = 0)
            }
            anEntry("origin/main") {
                author(alice)
                file("src/Service.kt", additions = 3, deletions = 1)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.authorStatistics.size() shouldBe 1
        result.authorStatistics[alice]!!.commitCount shouldBe 3
    }

    @Test
    fun `author statistics should track linesAdded and linesRemoved`() {
        // Given
        val alice = Author("Alice", "alice@example.com")
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                author(alice)
                file("src/Main.kt", additions = 10, deletions = 3)
            }
            anEntry("origin/main") {
                author(alice)
                file("src/Helper.kt", additions = 5, deletions = 2)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        val authorStat = result.authorStatistics[alice]!!
        authorStat.linesAdded shouldBe 15
        authorStat.linesRemoved shouldBe 5
    }

    @Test
    fun `author statistics should contain work items from commits`() {
        // Given
        val alice = Author("Alice", "alice@example.com")
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                author(alice)
                subject("feat: add login ABC-123")
                file("src/Main.kt", additions = 5, deletions = 0)
            }
            anEntry("origin/main") {
                author(alice)
                subject("fix: improve login ABC-123")
                file("src/Main.kt", additions = 3, deletions = 1)
            }
            anEntry("origin/main") {
                author(alice)
                subject("feat: add logout XYZ-456")
                file("src/Logout.kt", additions = 10, deletions = 0)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        val authorStat = result.authorStatistics[alice]!!
        authorStat.workItems shouldContainExactlyInAnyOrder setOf(
            WorkKey.Known("ABC-123"),
            WorkKey.Known("XYZ-456")
        )
    }

    @Test
    fun `author statistics should track churn by commit type`() {
        // Given
        val alice = Author("Alice", "alice@example.com")
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                author(alice)
                subject("F(login): add login")
                file("src/Login.kt", additions = 20, deletions = 0)
            }
            anEntry("origin/main") {
                author(alice)
                subject("F(logout): add logout")
                file("src/Logout.kt", additions = 15, deletions = 5)
            }
            anEntry("origin/main") {
                author(alice)
                subject("^ b(login): fix login bug")
                file("src/Login.kt", additions = 3, deletions = 2)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        val authorStat = result.authorStatistics[alice]!!
        authorStat.churnByCommitType[CommitType.FEATURE] shouldBe ChurnMetric(additions = 35, deletions = 5)
        authorStat.churnByCommitType[CommitType.FIX] shouldBe ChurnMetric(additions = 3, deletions = 2)
    }

    @Test
    fun `author statistics should include direct co-authors as collaborators`() {
        // Given
        val alice = Author("Alice", "alice@example.com")
        val bob = Author("Bob", "bob@example.com")
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                author(alice)
                subject("feat: add login")
                trailers("Co-authored-by" to "Bob <bob@example.com>")
                file("src/Login.kt", additions = 10, deletions = 0)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        val authorStat = result.authorStatistics[alice]!!
        authorStat.collaborators shouldContainExactlyInAnyOrder setOf(bob)
    }

    @Test
    fun `authors who work on the same work item should see each other as collaborators`() {
        // Given
        val alice = Author("Alice", "alice@example.com")
        val bob = Author("Bob", "bob@example.com")
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                author(alice)
                subject("feat: start login ABC-123")
                file("src/Login.kt", additions = 10, deletions = 0)
            }
            anEntry("origin/main") {
                author(bob)
                subject("feat: complete login ABC-123")
                file("src/Login.kt", additions = 5, deletions = 2)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.authorStatistics[alice]!!.collaborators shouldContainExactlyInAnyOrder setOf(bob)
        result.authorStatistics[bob]!!.collaborators shouldContainExactlyInAnyOrder setOf(alice)
    }

    @Test
    fun `collaborators should combine co-authors and shared work item authors and exclude self`() {
        // Given
        val alice = Author("Alice", "alice@example.com")
        val bob = Author("Bob", "bob@example.com")
        val charlie = Author("Charlie", "charlie@example.com")
        val gitLogContent = aGitLog {
            // Alice works on ABC-123 with Bob as co-author
            anEntry("origin/main") {
                author(alice)
                subject("feat: start feature ABC-123")
                trailers("Co-authored-by" to "Bob <bob@example.com>")
                file("src/Feature.kt", additions = 10, deletions = 0)
            }
            // Charlie also works on ABC-123 separately
            anEntry("origin/main") {
                author(charlie)
                subject("feat: continue feature ABC-123")
                file("src/Feature.kt", additions = 5, deletions = 0)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        // Alice should see both Bob (co-author) and Charlie (shared work item)
        result.authorStatistics[alice]!!.collaborators shouldContainExactlyInAnyOrder setOf(bob, charlie)
        // Charlie should see Alice (shared work item) but not Bob (who was just a co-author on Alice's commit)
        result.authorStatistics[charlie]!!.collaborators shouldContainExactlyInAnyOrder setOf(alice)
    }
}
