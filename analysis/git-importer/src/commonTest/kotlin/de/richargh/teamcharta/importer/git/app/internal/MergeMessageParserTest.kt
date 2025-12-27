package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api.BranchName
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MergeMessageParserTest {

    @Test
    fun `should extract branch name from standard merge message`() {
        val result = extractAllMergedBranches("Merge branch 'feature'")
        result shouldBe listOf(BranchName("feature"))
    }

    @Test
    fun `should extract branch name from merge into message`() {
        val result = extractAllMergedBranches("Merge branch 'feature' into main")
        result shouldBe listOf(BranchName("feature"))
    }

    @Test
    fun `should return empty for non-merge message`() {
        val result = extractAllMergedBranches("Add login feature")
        result shouldBe emptyList()
    }

    @Test
    fun `should extract branch name with slashes`() {
        val result = extractAllMergedBranches("Merge branch 'feat/login'")
        result shouldBe listOf(BranchName("feat/login"))
    }

    @Test
    fun `should extract all branch names from octopus merge`() {
        val result = extractAllMergedBranches("Merge branches 'feat1', 'feat2' and 'feat3'")
        result shouldBe listOf(
            BranchName("feat1"),
            BranchName("feat2"),
            BranchName("feat3")
        )
    }
}
