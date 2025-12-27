package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.Ref
import de.richargh.teamcharta.importer.gitfixtures.app.aGitLog
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class RefDetectionTest : FunSpec({

    test("should parse HEAD ref") {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                refHead("main")
            }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result.first().refs shouldContainExactlyInAnyOrder listOf(
            Ref.BranchTip("origin/main"),
            Ref.LocalHead("main")
        )
    }

    test("should parse tag ref") {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") { refTag("v1.0.0") }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result.first().refs shouldContainExactlyInAnyOrder listOf(
            Ref.BranchTip("origin/main"),
            Ref.Tag("v1.0.0")
        )
    }

    test("should parse branch ref without slash") {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/develop") { }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result.first().refs shouldContainExactly listOf(Ref.BranchTip("origin/develop"))
    }

    withData(
        nameFn = { "should parse nested branch ref: $it" },
        "origin/add-user",
        "origin/feat/add-login",
        "origin/fix/at-1234",
        "feature/user/profile",
        "release/v2.0/hotfix",
        "bugfix/JIRA-123/fix-null-pointer",
        "upstream/develop/experimental"
    ) { branchName ->
        // Given
        val gitLogContent = aGitLog {
            anEntry(branchName) { }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result.first().refs shouldContainExactly listOf(Ref.BranchTip(branchName))
    }

    test("should parse multiple refs") {
        // Given
        val gitLogContent = aGitLog {
            anEntry("origin/main") {
                refHead("main")
                refTag("v1.0.0")
            }
        }

        val testee = GitLogParser()

        // When
        val result = testee.parse(gitLogContent.lineSequence()).toList()

        // Then
        result.first().refs shouldContainExactlyInAnyOrder listOf(
            Ref.LocalHead("main"),
            Ref.BranchTip("origin/main"),
            Ref.Tag("v1.0.0")
        )
    }
})
