package de.richargh.teamcharta.shared

import de.richargh.teamcharta.shared.dto.AuthorStatsDto
import de.richargh.teamcharta.shared.dto.CommitTimelineDto
import de.richargh.teamcharta.shared.dto.GitMiningResultDto
import de.richargh.teamcharta.shared.dto.WorkItemDurationDto
import kotlinx.serialization.json.Json
import java.io.File

fun main(args: Array<String>) {
    val outputPath = args.firstOrNull() ?: "visualization/src/assets/data.json"

    val sampleData = generateSampleData()
    val json = Json {
        prettyPrint = true
    }
    val jsonString = json.encodeToString(sampleData)

    File(outputPath).apply {
        parentFile?.mkdirs()
        writeText(jsonString)
    }

    println("Generated sample data to: $outputPath")
}

fun generateSampleData(): GitMiningResultDto {
    val authors = listOf(
        AuthorStatsDto(
            name = "Alice Chen",
            commitCount = 245,
            linesAdded = 12500,
            linesDeleted = 4200,
            avgCommitSize = 68.2
        ),
        AuthorStatsDto(
            name = "Bob Martinez",
            commitCount = 189,
            linesAdded = 8900,
            linesDeleted = 3100,
            avgCommitSize = 63.5
        ),
        AuthorStatsDto(
            name = "Carol Smith",
            commitCount = 156,
            linesAdded = 7200,
            linesDeleted = 2800,
            avgCommitSize = 64.1
        ),
        AuthorStatsDto(
            name = "David Lee",
            commitCount = 98,
            linesAdded = 4500,
            linesDeleted = 1800,
            avgCommitSize = 64.3
        ),
        AuthorStatsDto(
            name = "Eve Johnson",
            commitCount = 67,
            linesAdded = 3100,
            linesDeleted = 950,
            avgCommitSize = 60.4
        )
    )

    val commitTimeline = generateCommitTimeline()
    val workItems = generateWorkItems()

    return GitMiningResultDto(
        authors = authors,
        commitTimeline = commitTimeline,
        workItems = workItems
    )
}

private fun generateCommitTimeline(): List<CommitTimelineDto> {
    val timeline = mutableListOf<CommitTimelineDto>()
    val authors = listOf("Alice Chen", "Bob Martinez", "Carol Smith", "David Lee", "Eve Johnson")
    val cumulativeCounts = mutableMapOf<String, Int>()
    authors.forEach { cumulativeCounts[it] = 0 }

    // Generate timeline for 6 months (Jan 2024 - Jun 2024)
    for (month in 1..6) {
        for (day in listOf(1, 8, 15, 22)) {
            val date = "2024-%02d-%02d".format(month, day)
            for (author in authors) {
                val increment = when (author) {
                    "Alice Chen" -> (8..15).random()
                    "Bob Martinez" -> (6..12).random()
                    "Carol Smith" -> (5..10).random()
                    "David Lee" -> (3..7).random()
                    else -> (2..5).random()
                }
                cumulativeCounts[author] = cumulativeCounts[author]!! + increment
                timeline.add(
                    CommitTimelineDto(
                        date = date,
                        cumulativeCount = cumulativeCounts[author]!!,
                        author = author
                    )
                )
            }
        }
    }
    return timeline
}

private fun generateWorkItems(): List<WorkItemDurationDto> {
    val types = listOf("Bug", "Feature", "Task", "Improvement")
    val workItems = mutableListOf<WorkItemDurationDto>()

    // Generate 50 work items across 6 months
    var itemNumber = 100
    for (month in 1..6) {
        for (i in 1..8) {
            val day = ((i - 1) * 3 + 1).coerceAtMost(28)
            val type = types[(itemNumber - 100) % types.size]
            val baseDuration = when (type) {
                "Bug" -> 2.0
                "Feature" -> 8.0
                "Task" -> 3.0
                else -> 4.0
            }
            val duration = baseDuration + (Math.random() * baseDuration * 0.5)
            val roundedDuration = (duration * 10).toLong() / 10.0

            workItems.add(
                WorkItemDurationDto(
                    key = "PROJ-$itemNumber",
                    type = type,
                    startDate = "2024-%02d-%02d".format(month, day),
                    durationDays = roundedDuration
                )
            )
            itemNumber++
        }
    }
    return workItems
}
