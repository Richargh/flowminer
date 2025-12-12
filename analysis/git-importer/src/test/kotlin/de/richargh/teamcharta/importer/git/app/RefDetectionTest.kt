package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.BranchName
import de.richargh.teamcharta.importer.git.app.api2.Ref
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RefDetectionTest {

    @Test
    fun `should parse HEAD ref`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("main") {
                refHead("main")
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactlyInAnyOrder listOf(
            Ref.BranchTip("main"),
            Ref.Head("main")
        )
    }

    @Test
    fun `should parse tag ref`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("main") { refTag("v1.0.0") }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactlyInAnyOrder listOf(
            Ref.BranchTip("main"),
            Ref.Tag("v1.0.0")
        )
    }

    @Test
    fun `should parse branch ref without slash`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("develop") {  }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(Ref.BranchTip("develop"))
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
            anEntry(branchName) {  }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactly listOf(Ref.BranchTip(branchName))
    }

    @Test
    fun `should parse multiple refs`() {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                refHead("main")
                refTag("v1.0.0")
            }
        }

        val testee = GitLogParser2()

        // When
        val result = testee.parse(gitLogContent.lineSequence())

        // Then
        result.commits.first().refs shouldContainExactlyInAnyOrder listOf(
            Ref.Head("main"),
            Ref.BranchTip("origin/main"),
            Ref.Tag("v1.0.0")
        )
    }
}
