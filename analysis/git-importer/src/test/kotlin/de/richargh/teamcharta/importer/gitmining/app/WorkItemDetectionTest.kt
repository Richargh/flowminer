package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.aGitLog
import de.richargh.teamcharta.importer.git.app.api.Author
import de.richargh.teamcharta.importer.git.app.api.WorkKey
import de.richargh.teamcharta.importer.gitmining.app.api.AuthorContribution
import de.richargh.teamcharta.importer.shared.time.app.testNow2025
import de.richargh.teamcharta.importer.shared.time.app.zoned
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Path

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
                authorDate("2024-01-10T10:00:00+01:00".zoned())
                file("src/Feature.kt", additions = 10, deletions = 0)
                file("src/Helper.kt", additions = 5, deletions = 0)
            }
            anEntry("origin/feature") {
                subject("ABC-123 second commit")
                authorDate("2024-01-11T10:00:00+01:00".zoned())
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
            Path.of("src/Feature.kt"),
            Path.of("src/Helper.kt"),
            Path.of("src/Service.kt")
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
}
