package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.api.FileChange
import de.richargh.teamcharta.importer.gitfixtures.app.api.aCommit
import de.richargh.teamcharta.importer.gitfixtures.app.api.hash
import de.richargh.teamcharta.importer.gitminingfixtures.app.api.aBranch
import de.richargh.teamcharta.importer.sharedfixtures.time.app.testNow2025
import de.richargh.teamcharta.importer.sharedfixtures.time.app.toInstant
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test
import parse

class GitLogMinerTest {

    @Test
    fun `should parse single commit into Commit object`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|abc123|parent1|2024-01-15T10:00:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            This is the commit body.
            -----FILES_START-----
            5	2	src/Main.kt
        """.trimIndent()

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.commits.all() shouldContainExactly listOf(aCommit {
            hash("abc123")
            author("John Doe", "john@example.com")
            date("2024-01-15T10:00:00+01:00".toInstant())
            message("Initial commit")
            parents("parent1")
            headRef("main")
            branchTip("origin/main")
            fileChanges(
                FileChange("src/Main.kt", additions = 5, deletions = 2)
            )
            certainBranch("origin/main")
            isOnCurrentBranch()
        })
    }

    @Test
    fun `should parse single commit into branch object`() {
        // Given
        val gitLogContent = """
            -----COMMIT_START-----
            HEAD -> main, origin/main, origin/HEAD|abc123|parent1|2024-01-15T10:00:00+01:00|John Doe|john@example.com|Initial commit
            -----BODY_START-----
            This is the commit body.
            -----FILES_START-----
            5	2	src/Main.kt
        """.trimIndent()

        val testee = GitLogMiner()

        // When
        val result = testee.parse(gitLogContent.lineSequence(), testNow2025)

        // Then
        result.branches.all() shouldContainExactly listOf(aBranch {
            name("origin/main")
            isCurrent()
            isStale()
            firstCommitHash("abc123".hash())
            firstCommitDate("2024-01-15T10:00:00+01:00".toInstant())
        })
    }
}