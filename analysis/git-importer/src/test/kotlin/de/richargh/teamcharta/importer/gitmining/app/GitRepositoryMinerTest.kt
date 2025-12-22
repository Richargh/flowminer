package de.richargh.teamcharta.importer.gitmining.app

import io.kotest.matchers.collections.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.time.Clock

class GitRepositoryMinerTest {
    private val repoPath = File("../..")

    @Test
    fun `should find at least one commit in current repository`() {
        // Given
        val testee = GitRepositoryMiner()

        // When
        val result = testee.parse(repoPath, since = "1 year ago", Clock.System.now())

        // Then
        result.commits.all().shouldNotBeEmpty()
    }
}