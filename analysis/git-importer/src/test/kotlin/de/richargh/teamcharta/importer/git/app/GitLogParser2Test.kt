package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.*
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GitLogParser2Test {

    @Test
    fun `should parse single commit into Commit object`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00:00+01:00
            subject==>> Initial commit
            parents==>> parent1
            refs==>> HEAD -> main
            -----BODY_START-----
            This is the commit body.
            -----FILES_START-----
            5	2	src/Main.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits shouldContainExactly listOf(aCommit {
            hash("abc123")
            author("John Doe", "john@example.com")
            date(ZonedDateTime.parse("2024-01-15T10:00:00+01:00"))
            message("Initial commit")
            parents("parent1")
            refs("HEAD -> main")
            fileChanges(
                FileChange("src/Main.kt", additions = 5, deletions = 2))
        })
    }

    @Test
    fun `should parse single commit into branch object`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc123
            author==>> John Doe
            authorMail==>> john@example.com
            authorDate==>> 2024-01-15T10:00:00+01:00
            subject==>> Initial commit
            parents==>> parent1
            refs==>> HEAD -> main
            -----BODY_START-----
            This is the commit body.
            -----FILES_START-----
            5	2	src/Main.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.branches.all() shouldContainExactly listOf(aBranch {
            name("main")
            firstCommitHash("abc123")
            firstCommitDate(ZonedDateTime.parse("2024-01-15T10:00:00+01:00"))
        })
    }
}