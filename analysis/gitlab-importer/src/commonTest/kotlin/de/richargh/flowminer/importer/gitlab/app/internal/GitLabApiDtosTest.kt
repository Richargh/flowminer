package de.richargh.flowminer.importer.gitlab.app.internal

import de.richargh.flowminer.importer.gitlab.app.dto.GitLabJobDto
import de.richargh.flowminer.importer.gitlab.app.dto.GitLabPipelineDto
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class GitLabApiDtosTest {

    @Test
    fun `shouldDeserializePipelineResponse`() {
        // given
        val jsonString = """{"id":1,"ref":"main","status":"success","created_at":"2024-01-01T10:00:00.000Z"}"""

        // when
        val dto = Json.decodeFromString<GitLabPipelineDto>(jsonString)

        // then
        dto.id shouldBe 1
        dto.ref shouldBe "main"
        dto.status shouldBe "success"
        dto.created_at shouldBe "2024-01-01T10:00:00.000Z"
    }

    @Test
    fun `shouldDeserializeJobResponse`() {
        // given
        val jsonString = """{"id":42,"name":"build","stage":"build","status":"success","started_at":"2024-01-01T10:01:00.000Z","finished_at":"2024-01-01T10:05:00.000Z","duration":240.0,"allow_failure":false}"""

        // when
        val dto = Json.decodeFromString<GitLabJobDto>(jsonString)

        // then
        dto.id shouldBe 42
        dto.name shouldBe "build"
        dto.stage shouldBe "build"
        dto.status shouldBe "success"
        dto.duration shouldBe 240.0
        dto.allow_failure shouldBe false
    }
}
