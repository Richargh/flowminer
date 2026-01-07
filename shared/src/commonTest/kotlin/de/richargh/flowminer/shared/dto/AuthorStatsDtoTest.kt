package de.richargh.flowminer.shared.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthorStatsDtoTest {

    @Test
    fun serialization_roundtrip_preserves_all_fields() {
        val original = AuthorStatsDto(
            name = "Alice",
            commitCount = 150,
            linesAdded = 5000,
            linesDeleted = 2000,
            avgCommitSize = 46.67
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<AuthorStatsDto>(json)

        assertEquals(original, deserialized)
    }
}
