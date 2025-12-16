package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.*
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GitLogParserTest {

    @Test
    fun `should parse single commit into Commit object`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|abc123|parent1|2024-01-15T10:00:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            This is the commit body.
            -----FILES_START-----
            5	2	src/Main.kt
        """.trimIndent()

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result shouldContainExactly listOf(aCommit {
            hash("abc123")
            author("John Doe", "john@example.com")
            date(ZonedDateTime.parse("2024-01-15T10:00:00+01:00"))
            message("Initial commit")
            parents("parent1")
            headRef("main")
            branchTip("origin/main")
            fileChanges(
                FileChange("src/Main.kt", additions = 5, deletions = 2))
            certainBranch("origin/main")
            isOnActiveBranch()
        })
    }
}