package de.richargh.teamcharta.shared.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class VisualizationDataDtoTest {

    @Test
    fun serialization_roundtrip_preserves_full_structure() {
        val original = GitMiningResultDto(
            authors = listOf(
                AuthorStatsDto(
                    name = "Alice",
                    commitCount = 150,
                    linesAdded = 5000,
                    linesDeleted = 2000,
                    avgCommitSize = 46.67
                )
            ),
            commitTimeline = listOf(
                CommitTimelineDto(
                    date = "2024-01-15",
                    cumulativeCount = 42,
                    author = "Alice"
                )
            ),
            workItems = listOf(
                WorkItemDurationDto(
                    key = "PROJ-123",
                    type = "Bug",
                    startDate = "2024-01-01",
                    durationDays = 5.5
                )
            )
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<GitMiningResultDto>(json)

        assertEquals(original, deserialized)
    }

    @Test
    fun serialization_handles_empty_lists() {
        val original = GitMiningResultDto(
            authors = emptyList(),
            commitTimeline = emptyList(),
            workItems = emptyList()
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<GitMiningResultDto>(json)

        assertEquals(original, deserialized)
    }
}
