package de.richargh.teamcharta.importer.git.app

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class GitTrailerExtractionTest {

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

}