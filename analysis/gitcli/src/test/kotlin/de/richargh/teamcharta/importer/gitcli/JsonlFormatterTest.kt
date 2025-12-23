package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.git.app.api.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class JsonlFormatterTest {

    @Test
    fun `formats single commit as JSONL`() {
        // TODO use commit builder
        // Given
        val commit = Commit(
            hash = CommitHash("abc123"),
            author = Author("Jane Doe", "jane@example.com"),
            date = Instant.parse("2024-01-15T10:30:00Z"),
            message = "Add feature X",
            parents = listOf(CommitHash("def456")),
            refs = emptyList(),
            fileChanges = listOf(
                FileChange(
                    path = "src/main.kt",
                    additions = 10,
                    deletions = 2,
                    isRename = false,
                    oldPath = null
                )
            ),
            trailers = emptyList(),
            coAuthors = emptySet(),
            commitType = CommitType.FEATURE,
            workKeys = listOf(WorkKey.Known("JIRA-123")),
            branchId = NamedBranchId.Certain(BranchName("main")),
            isOnCurrentBranch = true
        )

        // When
        val jsonl = JsonlFormatter.format(listOf(commit))

        // Then - single line, valid JSON object
        jsonl.lines().size shouldBe 1
        jsonl shouldContain "\"hash\":\"abc123\""
        jsonl shouldContain "\"authorName\":\"Jane Doe\""
        jsonl shouldContain "\"branchId\":{\"type\":\"certain\",\"name\":\"main\",\"tipCommit\":null}"
    }

    @Test
    fun `formats multiple commits as JSONL with one per line`() {
        // TODO use commit builder
        // Given
        val commit1 = Commit(
            hash = CommitHash("abc123"),
            author = Author("Jane", "jane@example.com"),
            date = Instant.parse("2024-01-15T10:30:00Z"),
            message = "First",
            parents = emptyList(),
            refs = emptyList(),
            fileChanges = emptyList(),
            trailers = emptyList(),
            coAuthors = emptySet(),
            commitType = CommitType.FEATURE,
            workKeys = emptyList(),
            branchId = NamedBranchId.Certain(BranchName("main")),
            isOnCurrentBranch = true
        )
        val commit2 = Commit(
            hash = CommitHash("def456"),
            author = Author("John", "john@example.com"),
            date = Instant.parse("2024-01-16T11:00:00Z"),
            message = "Second",
            parents = listOf(CommitHash("abc123")),
            refs = emptyList(),
            fileChanges = emptyList(),
            trailers = emptyList(),
            coAuthors = emptySet(),
            commitType = CommitType.FIX,
            workKeys = emptyList(),
            branchId = NamelessBranchId(CommitHash("tip123")),
            isOnCurrentBranch = false
        )

        // When
        val jsonl = JsonlFormatter.format(listOf(commit1, commit2))

        // Then
        val lines = jsonl.lines()
        lines.size shouldBe 2

        // Each line is a valid JSON object
        lines[0] shouldContain "\"hash\":\"abc123\""
        lines[1] shouldContain "\"hash\":\"def456\""

        // No array wrapper
        jsonl shouldNotContain "[\n"
        jsonl shouldNotContain "\n]"
    }

    @Test
    fun `formats empty list as empty string`() {
        // When
        val jsonl = JsonlFormatter.format(emptyList())

        // Then
        jsonl shouldBe ""
    }
}
