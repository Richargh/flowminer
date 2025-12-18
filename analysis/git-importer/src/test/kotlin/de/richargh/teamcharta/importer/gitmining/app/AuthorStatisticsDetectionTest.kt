package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.aGitLog
import de.richargh.teamcharta.importer.git.app.api.Author
import de.richargh.teamcharta.importer.shared.time.app.testNow2025
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
}
