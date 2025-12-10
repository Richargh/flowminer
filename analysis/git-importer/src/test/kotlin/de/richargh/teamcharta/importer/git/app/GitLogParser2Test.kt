package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.*
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GitLogParser2Test {

    @Test
    fun `should parse single commit with numstat into Commit object`() {
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
    fun `should extract trailers section`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> abc789
            author==>> Jane Smith
            authorMail==>> jane@example.com
            authorDate==>> 2024-01-17T09:00:00+01:00
            subject==>> Reviewed commit
            parents==>> def456
            refs==>>
            -----BODY_START-----
            Some changes.
            -----TRAILERS_START-----
            Co-authored-by: John Doe <john@example.com>
            Signed-off-by: Alice Wonder <alice@example.com>
            Reviewed-by: Bob Builder <bob@example.com>
            -----FILES_START-----
            3	1	src/File.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].trailers shouldContainExactly listOf(
            "Co-authored-by" to "John Doe <john@example.com>",
            "Signed-off-by" to "Alice Wonder <alice@example.com>",
            "Reviewed-by" to "Bob Builder <bob@example.com>"
        )
    }

    @Test
    fun `should extract co-authors from trailers section`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> def456
            author==>> Jane Smith
            authorMail==>> jane@example.com
            authorDate==>> 2024-01-16T14:30:00+01:00
            subject==>> Pair programming commit
            parents==>> abc123
            refs==>>
            -----BODY_START-----
            Added new feature together.
            -----TRAILERS_START-----
            Co-authored-by: John Doe <john@example.com>
            Co-authored-by: Alice Wonder <alice@example.com>
            -----FILES_START-----
            10	5	src/Feature.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].trailers shouldContainExactly listOf(
            "Co-authored-by" to "John Doe <john@example.com>",
            "Co-authored-by" to "Alice Wonder <alice@example.com>"
        )
        result.commits[0].coAuthors shouldContainExactly listOf(
            Author("John Doe", "john@example.com"),
            Author("Alice Wonder", "alice@example.com")
        )
    }

    @Test
    fun `should deduplicate co-authors with different trailer key variations`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            hash==>> xyz999
            author==>> Jane Smith
            authorMail==>> jane@example.com
            authorDate==>> 2024-01-18T11:00:00+01:00
            subject==>> Commit with duplicate co-authors
            parents==>> abc123
            refs==>>
            -----BODY_START-----
            Some work.
            -----TRAILERS_START-----
            Co-authored-by: John Doe <john@example.com>
            co-authored-by: John Doe <john@example.com>
            Co-Authored-By: John Doe <john@example.com>
            Co-Authored By: John Doe <john@example.com>
            Co-Authored by: John Doe <john@example.com>
            -----FILES_START-----
            1	1	src/File.kt
        """.trimIndent()

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].coAuthors shouldContainExactly listOf(
            Author("John Doe", "john@example.com")
        )
    }
}
