package de.richargh.flowminer.shared.dto

import de.richargh.flowminer.model.SerializablePipelineJobDto
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class CiMiningResultDtoTest {

    @Test
    fun `shouldSerializeToJsonAndBack`() {
        // given
        val dto = CiMiningResultDto(
            jobs = listOf(
                SerializablePipelineJobDto(
                    pipelineId = 1,
                    pipelineRef = "main",
                    pipelineStatus = "success",
                    pipelineCreatedAt = "2024-01-01T10:00:00Z",
                    jobId = 42,
                    jobName = "build",
                    jobStage = "build",
                    jobStatus = "success",
                    jobStartedAt = "2024-01-01T10:01:00Z",
                    jobFinishedAt = "2024-01-01T10:05:00Z",
                    jobDurationSeconds = 240.0,
                    jobAllowFailure = false
                )
            )
        )

        // when
        val json = Json.encodeToString(dto)
        val decoded = Json.decodeFromString<CiMiningResultDto>(json)

        // then
        decoded shouldBe dto
    }
}
