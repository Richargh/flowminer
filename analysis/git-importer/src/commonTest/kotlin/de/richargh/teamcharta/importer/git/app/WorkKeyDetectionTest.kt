package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.WorkKey
import de.richargh.teamcharta.importer.gitfixtures.app.aGitLog
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class WorkKeyDetectionTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "add login",
            "fix: add login",
            "feat: improve performance",
            "just some random message"
        ]
    )
    fun `should return empty list when no work key in message`(subject: String) {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") { subject(subject) }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result[0].workKeys.shouldBeEmpty()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "feat: add login #123",
            "fix #123 add login",
            "#123 add login",
            "add login (#123)",
            "add login [#123]"
        ]
    )
    fun `should detect single GitHub style work key`(subject: String) {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") { subject(subject) }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result[0].workKeys shouldContainExactlyInAnyOrder listOf(WorkKey.Known("#123"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "feat: add login ABC-123",
            "fix ABC-123 add login",
            "ABC-123 add login",
            "add login (ABC-123)",
            "add login [ABC-123]"
        ]
    )
    fun `should detect single Jira style work key`(subject: String) {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") { subject(subject) }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result[0].workKeys shouldContainExactlyInAnyOrder listOf(WorkKey.Known("ABC-123"))
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "feat: implement login #123 ABC-456 #789, #123|ABC-456|#789",
            "#1 #2, #1|#2",
            "ABC-1 DEF-2, ABC-1|DEF-2",
            "#100 XYZ-999, #100|XYZ-999"
        ]
    )
    fun `should detect multiple work keys from same message`(subject: String, expectedKeysStr: String) {
        // Given
        val expectedKeys = expectedKeysStr.split("|").map { WorkKey.Known(it) }
        val gitLogContent = aGitLog {
            anEntry("origin/main") { subject(subject) }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result[0].workKeys shouldContainExactlyInAnyOrder expectedKeys
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "PROJECT-1",
            "AB-99999",
            "TENLETTERS-1"
        ]
    )
    fun `should detect Jira keys with project codes 2-10 chars`(subject: String) {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") { subject(subject) }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result[0].workKeys.size shouldBe 1
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "A-123",
            "TOOLONGPROJECT-123"
        ]
    )
    fun `should not detect Jira keys with invalid project code length`(subject: String) {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") { subject(subject) }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result[0].workKeys.shouldBeEmpty()
    }
}
