package de.richargh.flowminer.shared.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkItemDurationDtoTest {

    @Test
    fun serialization_roundtrip_preserves_all_fields() {
        val original = WorkItemDurationDto(
            key = "PROJ-123",
            type = "Bug",
            startDate = "2024-01-01",
            durationDays = 5.5
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<WorkItemDurationDto>(json)

        assertEquals(original, deserialized)
    }
}
