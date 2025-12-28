package de.richargh.teamcharta.importer.git.app

import de.infix.testBalloon.framework.core.testSuite
import de.richargh.teamcharta.importer.git.app.api.Author
import de.richargh.teamcharta.importer.gitfixtures.app.aGitLog
import io.kotest.matchers.collections.shouldContainExactly

val CoAuthorExtractionTest by testSuite {

    test("should extract co-authors") {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                trailers(
                    "Co-authored-by" to "John Doe <john@example.com>",
                    "Co-authored-by" to "Alice Wonder <alice@example.com>"
                )
            }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result[0].trailers shouldContainExactly listOf(
            "Co-authored-by" to "John Doe <john@example.com>",
            "Co-authored-by" to "Alice Wonder <alice@example.com>"
        )
        result[0].coAuthors shouldContainExactly listOf(
            Author("John Doe", "john@example.com"),
            Author("Alice Wonder", "alice@example.com")
        )
    }

    test("should deduplicate co-authors with different trailer key variations") {
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

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result[0].coAuthors shouldContainExactly listOf(
            Author("John Doe", "john@example.com")
        )
    }
}
