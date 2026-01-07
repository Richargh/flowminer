package de.richargh.flowminer.importer.git.app

import de.infix.testBalloon.framework.core.testSuite
import de.richargh.flowminer.importer.git.app.api.WorkKey
import de.richargh.flowminer.importer.gitfixtures.app.aGitLog
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

val WorkKeyDetectionTest by testSuite {

    // Tests for empty/no work keys
    listOf(
        "add login",
        "fix: add login",
        "feat: improve performance",
        "just some random message"
    ).forEach { subject ->
        test("should return empty list for: $subject") {
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

    // Tests for GitHub key #123
    listOf(
        "feat: add login #123",
        "fix #123 add login",
        "#123 add login",
        "add login (#123)",
        "add login [#123]"
    ).forEach { subject ->
        test("should detect GitHub key #123 from: $subject") {
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
    }

    // Tests for Jira key ABC-123
    listOf(
        "feat: add login ABC-123",
        "fix ABC-123 add login",
        "ABC-123 add login",
        "add login (ABC-123)",
        "add login [ABC-123]"
    ).forEach { subject ->
        test("should detect Jira key ABC-123 from: $subject") {
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
    }

    // Tests for multiple keys
    data class MultipleWorkKeysTestCase(val subject: String, val expectedKeys: List<String>)

    listOf(
        MultipleWorkKeysTestCase("feat: implement login #123 ABC-456 #789", listOf("#123", "ABC-456", "#789")),
        MultipleWorkKeysTestCase("#1 #2", listOf("#1", "#2")),
        MultipleWorkKeysTestCase("ABC-1 DEF-2", listOf("ABC-1", "DEF-2")),
        MultipleWorkKeysTestCase("#100 XYZ-999", listOf("#100", "XYZ-999"))
    ).forEach { (subject, expectedKeyStrings) ->
        test("should detect multiple keys from: $subject") {
            // Given
            val expectedKeys = expectedKeyStrings.map { WorkKey.Known(it) }
            val gitLogContent = aGitLog {
                anEntry("origin/main") { subject(subject) }
            }

            val testee = GitLogParser()

            // When
            val result = testee.parse(gitLogContent.lineSequence()).toList()

            // Then
            result[0].workKeys shouldContainExactlyInAnyOrder expectedKeys
        }
    }

    // Tests for valid Jira project codes
    listOf(
        "PROJECT-1",
        "AB-99999",
        "TENLETTERS-1"
    ).forEach { subject ->
        test("should detect Jira key with valid project code: $subject") {
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
    }

    // Tests for invalid Jira project codes
    listOf(
        "A-123",
        "TOOLONGPROJECT-123"
    ).forEach { subject ->
        test("should not detect Jira key with invalid project code: $subject") {
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
}
