package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.BranchCreated
import de.richargh.teamcharta.importer.git.app.api.BranchMerged
import de.richargh.teamcharta.importer.git.app.api.CommitMade
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class GitLogParserTest {

    @Test
    fun `should parse simple commit events from git log`() {
        // Given
        val gitLogContent = """
            abc123||John Doe|2024-01-15T10:00:00Z|HEAD -> main, origin/main|Initial commit

            def456|abc123|Jane Smith|2024-01-16T11:00:00Z|main|Add feature
            1	0	src/Feature.kt
        """.trimIndent()

        val parser = GitLogParser()

        // When
        val result = parser.parseLog(gitLogContent, "main")

        // Then
        result.events shouldHaveSize 2
        result.defaultBranch shouldBe "main"

        val firstEvent = result.events[0]
        firstEvent.shouldBeInstanceOf<CommitMade>()
        firstEvent.commitHash shouldBe "abc123"
        firstEvent.author shouldBe "John Doe"
        firstEvent.message shouldBe "Initial commit"
        firstEvent.branchName shouldBe "main"

        val secondEvent = result.events[1]
        secondEvent.shouldBeInstanceOf<CommitMade>()
        secondEvent.commitHash shouldBe "def456"
        secondEvent.author shouldBe "Jane Smith"
    }

    @Test
    fun `should detect merge events`() {
        // Given
        val gitLogContent = """
            merge123|abc123 def456|John Doe|2024-01-20T15:00:00Z|HEAD -> main|Merge branch 'feature-x' into main

            def456|abc123|Jane Smith|2024-01-18T12:00:00Z|feature-x|Add feature x
            2	1	src/FeatureX.kt
            abc123||John Doe|2024-01-15T10:00:00Z|main|Initial commit

        """.trimIndent()

        val parser = GitLogParser()

        // When
        val result = parser.parseLog(gitLogContent, "main")

        // Then
        val mergeEvents = result.events.filterIsInstance<BranchMerged>()
        mergeEvents shouldHaveSize 1

        val mergeEvent = mergeEvents[0]
        mergeEvent.commitHash shouldBe "merge123"
        mergeEvent.branchName shouldBe "feature-x"
        mergeEvent.targetBranch shouldBe "main"
        mergeEvent.mergeMessage shouldBe "Merge branch 'feature-x' into main"
    }

    @Test
    fun `should detect branch creation events`() {
        // Given
        val gitLogContent = """
            def456|abc123|Jane Smith|2024-01-18T12:00:00Z|HEAD -> feature-x|Add feature x
            1	0	src/FeatureX.kt
            abc123||John Doe|2024-01-15T10:00:00Z|main, origin/main|Initial commit

        """.trimIndent()

        val parser = GitLogParser()

        // When
        val result = parser.parseLog(gitLogContent, "main")

        // Then
        val branchCreatedEvents = result.events.filterIsInstance<BranchCreated>()
        branchCreatedEvents shouldHaveSize 1

        val branchEvent = branchCreatedEvents[0]
        branchEvent.branchName shouldBe "feature-x"
        branchEvent.commitHash shouldBe "def456"
        branchEvent.author shouldBe "Jane Smith"
        branchEvent.parentBranch shouldBe "main"
    }

    @Test
    fun `should handle multiple branches and their lifecycle`() {
        // Given
        val gitLogContent = """
            merge789|ghi012 jkl345|John Doe|2024-01-25T16:00:00Z|HEAD -> main|Merge branch 'feature-y' into main

            jkl345|ghi012|Bob Builder|2024-01-24T14:00:00Z|feature-y|Complete feature y
            3	2	src/FeatureY.kt
            merge456|abc123 def456|John Doe|2024-01-20T15:00:00Z|main|Merge branch 'feature-x' into main

            ghi012|abc123|Bob Builder|2024-01-19T13:00:00Z|feature-y|Start feature y
            1	0	src/FeatureY.kt
            def456|abc123|Jane Smith|2024-01-18T12:00:00Z|feature-x|Add feature x
            2	1	src/FeatureX.kt
            abc123||John Doe|2024-01-15T10:00:00Z|main, origin/main|Initial commit

        """.trimIndent()

        val parser = GitLogParser()

        // When
        val result = parser.parseLog(gitLogContent, "main")

        // Then
        result.events.filterIsInstance<BranchCreated>() shouldHaveSize 2
        result.events.filterIsInstance<BranchMerged>() shouldHaveSize 2
        result.events.filterIsInstance<CommitMade>() shouldHaveSize 6 // All 6 commits including merges

        // Verify branch lifecycle for feature-x
        val featureXEvents = result.events.filter {
            (it as? BranchCreated)?.branchName == "feature-x" ||
            (it as? CommitMade)?.branchName == "feature-x" ||
            (it as? BranchMerged)?.branchName == "feature-x"
        }
        featureXEvents shouldHaveSize 3 // created, commit, merged
    }

    @Test
    fun `should sort events by timestamp`() {
        // Given
        val gitLogContent = """
            def456|abc123|Jane Smith|2024-01-18T12:00:00Z|feature-x|Add feature x

            abc123||John Doe|2024-01-15T10:00:00Z|main|Initial commit

            ghi789|def456|Bob Builder|2024-01-20T09:00:00Z|feature-x|Update feature x

        """.trimIndent()

        val parser = GitLogParser()

        // When
        val result = parser.parseLog(gitLogContent, "main")

        // Then
        val commits = result.events.filterIsInstance<CommitMade>()
        commits[0].commitHash shouldBe "abc123" // Earliest
        commits[1].commitHash shouldBe "def456" // Middle
        commits[2].commitHash shouldBe "ghi789" // Latest
    }
}
