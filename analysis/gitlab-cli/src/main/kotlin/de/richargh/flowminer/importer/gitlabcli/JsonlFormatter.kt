package de.richargh.flowminer.importer.gitlabcli

import de.richargh.flowminer.importer.gitlab.app.api.PipelineJob
import de.richargh.flowminer.importer.gitlab.app.dto.toDto
import kotlinx.serialization.json.Json
import java.io.File

fun writeJobsAsJsonl(jobs: List<PipelineJob>, outputFile: File) {
    val json = Json { encodeDefaults = true }
    val lines = jobs.map { job ->
        json.encodeToString(job.toDto())
    }
    outputFile.writeText(lines.joinToString("\n", postfix = "\n"))
}
