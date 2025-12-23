package de.richargh.teamcharta.importer.git.app.api

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Instant

class CommitDtoConversionTest {

    @Test
    fun `Commit converts to CommitDto and back with equality`() {
        // Given
        val original = Commit(
            hash = CommitHash("abc123"),
            author = Author("Jane Doe", "jane@example.com"),
            date = Instant.parse("2024-01-15T10:30:00Z"),
            message = "Add feature X",
            parents = listOf(CommitHash("def456")),
            refs = listOf(Ref.BranchTip("main")),
            fileChanges = listOf(
                FileChange(
                    path = "src/main.kt",
                    additions = 10,
                    deletions = 2,
                    isRename = false,
                    oldPath = null
                )
            ),
            trailers = listOf("Signed-off-by" to "Jane Doe"),
            coAuthors = setOf(Author("John Smith", "john@example.com")),
            commitType = CommitType.FEATURE,
            workKeys = listOf(WorkKey.Known("JIRA-123"), WorkKey.Unknown),
            branchId = NamedBranchId.Certain(BranchName("main")),
            isOnCurrentBranch = true
        )

        // When
        val dto = original.toDto()
        val converted = dto.toCommit()

        // Then - roundtrip should preserve essential fields
        converted.hash shouldBe original.hash
        converted.author shouldBe original.author
        converted.date shouldBe original.date
        converted.message shouldBe original.message
        converted.parents shouldBe original.parents
        converted.fileChanges shouldBe original.fileChanges
        converted.commitType shouldBe original.commitType
        converted.workKeys shouldBe original.workKeys
        converted.branchId shouldBe original.branchId
        converted.isOnCurrentBranch shouldBe original.isOnCurrentBranch
        converted.isMerge shouldBe original.isMerge
    }

    @Test
    fun `Commit with nameless branch converts correctly`() {
        // Given
        val original = Commit(
            hash = CommitHash("abc123"),
            author = Author("Jane Doe", "jane@example.com"),
            date = Instant.parse("2024-01-15T10:30:00Z"),
            message = "Commit on nameless branch",
            parents = listOf(CommitHash("def456"), CommitHash("ghi789")),
            refs = emptyList(),
            fileChanges = emptyList(),
            trailers = emptyList(),
            coAuthors = emptySet(),
            commitType = CommitType.UNKNOWN,
            workKeys = emptyList(),
            branchId = NamelessBranchId(CommitHash("tip123")),
            isOnCurrentBranch = false
        )

        // When
        val dto = original.toDto()
        val converted = dto.toCommit()

        // Then
        converted.branchId shouldBe original.branchId
        converted.isMerge shouldBe true
    }

    @Test
    fun `Commit with inferred branch converts correctly`() {
        // Given
        val original = Commit(
            hash = CommitHash("abc123"),
            author = Author("Jane Doe", "jane@example.com"),
            date = Instant.parse("2024-01-15T10:30:00Z"),
            message = "Commit on inferred branch",
            parents = emptyList(),
            refs = emptyList(),
            fileChanges = emptyList(),
            trailers = emptyList(),
            coAuthors = emptySet(),
            commitType = CommitType.FIX,
            workKeys = listOf(WorkKey.Known("BUG-456")),
            branchId = NamedBranchId.Inferred(BranchName("feature-x")),
            isOnCurrentBranch = true
        )

        // When
        val dto = original.toDto()
        val converted = dto.toCommit()

        // Then
        converted.branchId shouldBe original.branchId
    }
}
