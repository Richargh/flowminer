package de.richargh.teamcharta.importer.gitmining.app

import io.kotest.matchers.collections.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.io.File

class GitRepositoryParserTest {
    private val repoPath = File("../..")

    @Test
    fun `should find at least one commit in current repository`() {
        // Given
        val testee = GitRepositoryParser()

        // When
        val result = buildList {
            testee.parse(repoPath, since = "1 year ago") { commits ->
                addAll(commits.toList())
            }
        }

        // Then
        result.shouldNotBeEmpty()
    }
}