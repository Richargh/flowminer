package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.api.toCommit
import de.richargh.teamcharta.importer.gitmining.app.api.Commits
import de.richargh.teamcharta.importer.gitmining.app.api.GitMiningResult
import de.richargh.teamcharta.model.CommitDto
import de.richargh.teamcharta.model.SerializableCommitDto
import de.richargh.teamcharta.shared.dto.AuthorStatsDto
import de.richargh.teamcharta.shared.dto.CommitTimelineDto
import de.richargh.teamcharta.shared.dto.GitMiningResultDto
import de.richargh.teamcharta.shared.dto.WorkItemDurationDto
import kotlinx.serialization.json.Json
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalJsExport::class)
@JsExport
fun mineCommitsFromJsonl(jsonlContent: String): GitMiningResultDto {
    val commits = jsonlContent
        .lineSequence()
        .filter { it.isNotBlank() }
        .map { json.decodeFromString<SerializableCommitDto>(it) }
        .toList()
    return mineCommitsInternal(commits)
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun mineCommits(commits: Array<CommitDto>): GitMiningResultDto {
    return mineCommitsInternal(commits.toList())
}

private fun mineCommitsInternal(commits: List<CommitDto>): GitMiningResultDto {
    val miner = GitLogMiner()
    @OptIn(ExperimentalTime::class)
    val currentDate = Clock.System.now()

    val domainCommits = commits.map { it.toCommit() }
    val result = miner.mine(domainCommits.asSequence(), currentDate)

    return toVisualizationData(result)
}

private fun toVisualizationData(result: GitMiningResult): GitMiningResultDto {
    val authors = result.authorStatistics.all().map { stat ->
        val totalLines = stat.linesAdded + stat.linesRemoved
        val avgCommitSize = if (stat.commitCount > 0) totalLines.toDouble() / stat.commitCount else 0.0
        AuthorStatsDto(
            name = stat.author.name,
            commitCount = stat.commitCount,
            linesAdded = stat.linesAdded,
            linesDeleted = stat.linesRemoved,
            avgCommitSize = avgCommitSize
        )
    }

    val timeline = buildCommitTimeline(result.commits)

    val workItems = result.workItems.all().map { item ->
        WorkItemDurationDto(
            key = item.workKey.toString(),
            type = "WorkItem",
            startDate = item.firstCommitDate.toString(),
            durationDays = item.duration.inWholeDays.toDouble()
        )
    }

    return GitMiningResultDto(
        authors = authors,
        commitTimeline = timeline,
        workItems = workItems
    )
}

private fun buildCommitTimeline(commits: Commits): List<CommitTimelineDto> {
    if (commits.isEmpty()) return emptyList()

    val sortedCommits = commits.all().sortedBy { it.date }
    var cumulativeCount = 0

    return sortedCommits.map { commit ->
        cumulativeCount++
        CommitTimelineDto(
            date = commit.date.toString().substringBefore('T'),
            cumulativeCount = cumulativeCount,
            author = commit.author.name
        )
    }
}
