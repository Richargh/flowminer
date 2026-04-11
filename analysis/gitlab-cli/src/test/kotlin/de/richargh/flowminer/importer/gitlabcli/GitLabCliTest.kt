package de.richargh.flowminer.importer.gitlabcli

import de.richargh.flowminer.importer.gitlabfixtures.aPipelineJob
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class GitLabCliTest {

    @Test
    fun shouldWriteJsonlOutput(@TempDir tempDir: Path): Unit = runBlocking {
        // given
        val outputFile = tempDir.resolve("output.jsonl")
        val jobs = listOf(
            aPipelineJob { pipelineId = 1; jobName = "build"; jobId = 10 },
            aPipelineJob { pipelineId = 1; jobName = "test"; jobId = 11 }
        )

        // when
        writeJobsAsJsonl(jobs, outputFile.toFile())

        // then
        val lines = outputFile.readText().trim().split("\n")
        lines.size shouldBe 2
        lines[0] shouldContain """"jobName":"build""""
        lines[1] shouldContain """"jobName":"test""""
    }
}
