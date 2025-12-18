package de.richargh.teamcharta.shared.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CommitTimelineDtoTest {

    @Test
    fun serialization_roundtrip_preserves_all_fields() {
        val original = CommitTimelineDto(
            date = "2024-01-15",
            cumulativeCount = 42,
            author = "Bob"
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<CommitTimelineDto>(json)

        assertEquals(original, deserialized)
    }
}
