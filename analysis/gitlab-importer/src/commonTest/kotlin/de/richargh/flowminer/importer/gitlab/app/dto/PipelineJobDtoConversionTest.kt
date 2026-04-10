package de.richargh.flowminer.importer.gitlab.app.dto

import de.richargh.flowminer.importer.gitlab.app.api.JobStatus
import de.richargh.flowminer.importer.gitlab.app.api.PipelineStatus
import de.richargh.flowminer.importer.gitlabfixtures.aPipelineJob
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PipelineJobDtoConversionTest {

    @Test
    fun `shouldConvertPipelineJobToDto`() {
        // given
        val job = aPipelineJob {
            pipelineId = 10
            pipelineRef = "feature/foo"
            pipelineStatus = PipelineStatus.success
            jobId = 99
            jobName = "build"
            jobStage = "build-stage"
            jobStatus = JobStatus.success
            jobDurationSeconds = 120.0
            jobAllowFailure = false
        }

        // when
        val dto = job.toDto()

        // then
        dto.pipelineId shouldBe 10
        dto.pipelineRef shouldBe "feature/foo"
        dto.pipelineStatus shouldBe "success"
        dto.jobId shouldBe 99
        dto.jobName shouldBe "build"
        dto.jobStage shouldBe "build-stage"
        dto.jobStatus shouldBe "success"
        dto.jobDurationSeconds shouldBe 120.0
        dto.jobAllowFailure shouldBe false
    }
}