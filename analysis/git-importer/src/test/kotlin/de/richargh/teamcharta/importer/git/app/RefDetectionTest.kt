package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.Ref
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RefDetectionTest {

    @Test
    fun `should parse HEAD ref`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry { headRef("main") }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(Ref.Head("main"))
    }

    @Test
    fun `should parse tag ref`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry { tag("v1.0.0") }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(Ref.Tag("v1.0.0"))
    }

    @Test
    fun `should parse branch ref without slash`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry { branch("develop") }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(Ref.Branch("develop"))
    }

    @ParameterizedTest(name = "should parse nested branch ref: {0}")
    @CsvSource(
        "origin/add-user",
        "origin/feat/add-login",
        "origin/fix/at-1234",
        "feature/user/profile",
        "release/v2.0/hotfix",
        "bugfix/JIRA-123/fix-null-pointer",
        "upstream/develop/experimental"
    )
    fun `should parse nested branch refs`(branchName: String) {
        // Given
        val gitLogContent = aGitLog {
            anEntry { branch(branchName) }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(Ref.Branch(branchName))
    }

    @Test
    fun `should parse multiple refs`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry {
                headRef("main")
                branch("origin/main")
                tag("v1.0.0")
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(
            Ref.Head("main"),
            Ref.Branch("origin/main"),
            Ref.Tag("v1.0.0")
        )
    }

    @Test
    fun `should handle empty refs`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry {}
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldHaveSize 0
    }
}
