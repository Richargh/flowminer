package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.WorkKey
import de.richargh.teamcharta.importer.gitfixtures.app.aGitLog
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class WorkKeyDetectionTest : FunSpec({

    withData(
        nameFn = { "should return empty list for: $it" },
        "add login",
        "fix: add login",
        "feat: improve performance",
        "just some random message"
    ) { subject ->
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

    withData(
        nameFn = { "should detect GitHub key #123 from: $it" },
        "feat: add login #123",
        "fix #123 add login",
        "#123 add login",
        "add login (#123)",
        "add login [#123]"
    ) { subject ->
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

    withData(
        nameFn = { "should detect Jira key ABC-123 from: $it" },
        "feat: add login ABC-123",
        "fix ABC-123 add login",
        "ABC-123 add login",
        "add login (ABC-123)",
        "add login [ABC-123]"
    ) { subject ->
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

    data class MultipleWorkKeysTestCase(val subject: String, val expectedKeys: List<String>)

    withData(
        nameFn = { "should detect multiple keys from: ${it.subject}" },
        MultipleWorkKeysTestCase("feat: implement login #123 ABC-456 #789", listOf("#123", "ABC-456", "#789")),
        MultipleWorkKeysTestCase("#1 #2", listOf("#1", "#2")),
        MultipleWorkKeysTestCase("ABC-1 DEF-2", listOf("ABC-1", "DEF-2")),
        MultipleWorkKeysTestCase("#100 XYZ-999", listOf("#100", "XYZ-999"))
    ) { (subject, expectedKeyStrings) ->
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

    withData(
        nameFn = { "should detect Jira key with valid project code: $it" },
        "PROJECT-1",
        "AB-99999",
        "TENLETTERS-1"
    ) { subject ->
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

    withData(
        nameFn = { "should not detect Jira key with invalid project code: $it" },
        "A-123",
        "TOOLONGPROJECT-123"
    ) { subject ->
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
})
