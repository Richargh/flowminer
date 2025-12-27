package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.gitfixtures.app.aGitLog
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class GitTrailerExtractionTest {

    @Test
    fun `should extract trailers section`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                trailers(
                    "Co-authored-by" to "John Doe <john@example.com>",
                    "Signed-off-by" to "Alice Wonder <alice@example.com>",
                    "Reviewed-by" to "Bob Builder <bob@example.com>"
                )
            }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result[0].trailers shouldContainExactly listOf(
            "Co-authored-by" to "John Doe <john@example.com>",
            "Signed-off-by" to "Alice Wonder <alice@example.com>",
            "Reviewed-by" to "Bob Builder <bob@example.com>"
        )
    }
}
