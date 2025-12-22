package de.richargh.teamcharta.importer.gitmining.app


import de.richargh.teamcharta.importer.gitfixtures.app.aGitLog
import de.richargh.teamcharta.importer.sharedfixtures.time.app.testNow2025
import de.richargh.teamcharta.importer.sharedfixtures.time.app.toInstant
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

class WorkItemDurationTest {
    @Test
    fun `WorkItem should calculate duration from first to last commit`() {
        // Given - 3 commits over 5 days
        val gitLogContent = aGitLog {
            anEntry("origin/feature") {
                subject("ABC-123 first commit")
                authorDate("2024-01-10T10:00:00+01:00".toInstant())
                file("src/Feature.kt", additions = 10, deletions = 0)
            }
            anEntry("origin/feature") {
                subject("ABC-123 second commit")
                authorDate("2024-01-12T10:00:00+01:00".toInstant())
                file("src/Feature.kt", additions = 5, deletions = 0)
            }
            anEntry("origin/feature") {
                subject("ABC-123 third commit")
                authorDate("2024-01-15T10:00:00+01:00".toInstant())
                file("src/Feature.kt", additions = 3, deletions = 0)
            }
        }

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        val workItem = result.workItems["ABC-123"]!!
        workItem.firstCommitDate shouldBe "2024-01-10T10:00:00+01:00".toInstant()
        workItem.lastCommitDate shouldBe "2024-01-15T10:00:00+01:00".toInstant()
        workItem.duration shouldBe 5.days
    }
}