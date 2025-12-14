package de.richargh.teamcharta.importer.git.app

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
        val result = testee.parse(repoPath, since = "1 year ago")

        // Then
        result.commits.all().shouldNotBeEmpty()
    }
}
