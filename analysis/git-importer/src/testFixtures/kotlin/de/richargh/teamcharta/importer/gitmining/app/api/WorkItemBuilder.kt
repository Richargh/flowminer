package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.git.app.api.CommitType
import de.richargh.teamcharta.importer.git.app.api.WorkKey
import java.nio.file.Path
import java.time.ZonedDateTime

class WorkItemBuilder {
    private var workKey: WorkKey = WorkKey.Known("DEFAULT-1")
    private var linesAdded: Int = 0
    private var linesRemoved: Int = 0
    private var firstCommitDate: ZonedDateTime = ZonedDateTime.parse("2024-01-01T00:00:00+01:00")
    private var lastCommitDate: ZonedDateTime = ZonedDateTime.parse("2024-01-01T00:00:00+01:00")
    private var filesChanged: Set<Path> = emptySet()
    private var contributions: List<AuthorContribution> = emptyList()
    private var linesByType: Map<CommitType, Int> = emptyMap()

    fun workKey(workKey: WorkKey) = apply { this.workKey = workKey }
    fun workKey(key: String) = apply { this.workKey = WorkKey.Known(key) }
    fun linesAdded(lines: Int) = apply { this.linesAdded = lines }
    fun linesRemoved(lines: Int) = apply { this.linesRemoved = lines }
    fun firstCommitDate(date: ZonedDateTime) = apply { this.firstCommitDate = date }
    fun lastCommitDate(date: ZonedDateTime) = apply { this.lastCommitDate = date }
    fun filesChanged(files: Set<Path>) = apply { this.filesChanged = files }
    fun contributions(contributions: List<AuthorContribution>) = apply { this.contributions = contributions }
    fun linesByType(linesByType: Map<CommitType, Int>) = apply { this.linesByType = linesByType }

    fun build(): WorkItem = WorkItem(
        workKey = workKey,
        linesAdded = linesAdded,
        linesRemoved = linesRemoved,
        firstCommitDate = firstCommitDate,
        lastCommitDate = lastCommitDate,
        filesChanged = filesChanged,
        contributions = contributions,
        absoluteChurnByType = linesByType
    )
}

fun aWorkItem(block: WorkItemBuilder.() -> Unit = {}): WorkItem =
    WorkItemBuilder().apply(block).build()
