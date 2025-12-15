package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.Author
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class CoAuthorExtractionTest {

    @Test
    fun `should extract co-authors`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                trailers(
                    "Co-authored-by" to "John Doe <john@example.com>",
                    "Co-authored-by" to "Alice Wonder <alice@example.com>"
                )
            }
        }

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
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                trailers(
                    "Co-authored-by" to "John Doe <john@example.com>",
                    "co-authored-by" to "John Doe <john@example.com>",
                    "Co-Authored-By" to "John Doe <john@example.com>",
                    "Co-Authored By" to "John Doe <john@example.com>",
                    "Co-Authored by" to "John Doe <john@example.com>"
                )
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits[0].coAuthors shouldContainExactly listOf(
            Author("John Doe", "john@example.com")
        )
    }
}
