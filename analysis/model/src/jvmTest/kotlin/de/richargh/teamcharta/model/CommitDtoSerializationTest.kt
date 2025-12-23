package de.richargh.teamcharta.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class CommitDtoSerializationTest {

    @Test
    fun `CommitDto can be serialized to JSON`() {
        // Given
        val commitDto: CommitDto = SerializableCommitDto(
            hash = "abc123",
            authorName = "Jane Doe",
            authorEmail = "jane@example.com",
            date = "2024-01-15T10:30:00Z",
            message = "Add feature X",
            parents = listOf("def456"),
            branchId = SerializableBranchIdDto(
                type = "certain",
                name = "main",
                tipCommit = null
            ),
            workKeys = listOf("JIRA-123", null),
            commitType = "FEATURE",
            fileChanges = listOf(
                SerializableFileChangeDto(
                    path = "src/main.kt",
                    additions = 10,
                    deletions = 2,
                    isRename = false,
                    oldPath = null
                )
            ),
            isOnCurrentBranch = true,
            isMerge = false
        )

        // When
        val json = Json.encodeToString(commitDto as SerializableCommitDto)

        // Then
        json shouldNotContain "\n"  // JSONL format: single line
        json shouldBe """{"hash":"abc123","authorName":"Jane Doe","authorEmail":"jane@example.com","date":"2024-01-15T10:30:00Z","message":"Add feature X","parents":["def456"],"branchId":{"type":"certain","name":"main","tipCommit":null},"workKeys":["JIRA-123",null],"commitType":"FEATURE","fileChanges":[{"path":"src/main.kt","additions":10,"deletions":2,"isRename":false,"oldPath":null}],"isOnCurrentBranch":true,"isMerge":false}"""
    }

    @Test
    fun `multiple CommitDtos can be serialized to JSONL format`() {
        // Given
        val commits = listOf(
            SerializableCommitDto(
                hash = "abc123",
                authorName = "Jane",
                authorEmail = "jane@example.com",
                date = "2024-01-15T10:30:00Z",
                message = "First commit",
                parents = emptyList(),
                branchId = SerializableBranchIdDto(type = "certain", name = "main", tipCommit = null),
                workKeys = emptyList(),
                commitType = "FEATURE",
                fileChanges = emptyList(),
                isOnCurrentBranch = true,
                isMerge = false
            ),
            SerializableCommitDto(
                hash = "def456",
                authorName = "John",
                authorEmail = "john@example.com",
                date = "2024-01-16T11:00:00Z",
                message = "Second commit",
                parents = listOf("abc123"),
                branchId = SerializableBranchIdDto(type = "nameless", name = null, tipCommit = "abc123"),
                workKeys = listOf("JIRA-456"),
                commitType = "FIX",
                fileChanges = emptyList(),
                isOnCurrentBranch = false,
                isMerge = false
            )
        )

        // When
        val jsonl = commits.joinToString("\n") { Json.encodeToString(it) }

        // Then
        val lines = jsonl.lines()
        lines.size shouldBe 2
        // Each line should be a valid JSON object (starts with { not [)
        lines.forEach { line ->
            line.trim().first() shouldBe '{'
            line.trim().last() shouldBe '}'
        }
    }
}
