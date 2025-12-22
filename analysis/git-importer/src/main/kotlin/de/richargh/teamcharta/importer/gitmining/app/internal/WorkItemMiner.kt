package de.richargh.teamcharta.importer.gitmining.app.internal

import de.richargh.teamcharta.importer.git.app.api.Author
import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.git.app.api.CommitType
import de.richargh.teamcharta.importer.git.app.api.WorkKey
import de.richargh.teamcharta.importer.gitmining.app.api.AuthorContribution
import de.richargh.teamcharta.importer.gitmining.app.api.WorkItem
import de.richargh.teamcharta.importer.gitmining.app.api.WorkItems
import java.nio.file.Path
import java.time.ZonedDateTime

fun extractWorkItems(commits: List<Commit>): WorkItems {
    val workItems = mutableMapOf<WorkKey, MutableWorkKey>()

    for (commit in commits) {
        val workKeys = commit.workKeys.ifEmpty { listOf(WorkKey.Unknown) }
        for (workKey in workKeys) {
            workItems
                .getOrPut(workKey) { MutableWorkKey(workKey, commit.date) }
                .add(commit)
        }
    }

    return WorkItems(workItems.values.map(MutableWorkKey::toWorkItem))
}

private class MutableWorkKey(
    private val workKey: WorkKey,
    private var firstCommitDate: ZonedDateTime
) {
    private var linesAdded = 0
    private var linesRemoved = 0
    private var lastCommitDate: ZonedDateTime = firstCommitDate
    private val filesChanged = mutableSetOf<Path>()
    private val fileTouchCount = mutableMapOf<Path, Int>()
    private val absoluteChurnByAuthor = mutableMapOf<Author, Int>()
    private val absoluteChurnByType = mutableMapOf<CommitType, Int>()
    private var commitCount = 0
    private val collaborators = mutableSetOf<Author>()

    fun add(commit: Commit) {
        commitCount++
        collaborators.add(commit.author)
        collaborators.addAll(commit.coAuthors)

        var commitLinesChanged = 0
        for (fileChange in commit.fileChanges) {
            linesAdded += fileChange.additions
            linesRemoved += fileChange.deletions
            val path = Path.of(fileChange.path)
            filesChanged.add(path)
            fileTouchCount[path] = fileTouchCount.getOrDefault(path, 0) + 1
            commitLinesChanged += fileChange.additions + fileChange.deletions
        }

        absoluteChurnByAuthor[commit.author] = absoluteChurnByAuthor.getOrDefault(commit.author, 0) + commitLinesChanged
        absoluteChurnByType[commit.commitType] = absoluteChurnByType.getOrDefault(commit.commitType, 0) + commitLinesChanged

        val commitDate = commit.date
        if (commitDate.isBefore(firstCommitDate)) {
            firstCommitDate = commitDate
        }
        if (commitDate.isAfter(lastCommitDate)) {
            lastCommitDate = commitDate
        }
    }

    fun toWorkItem(): WorkItem = WorkItem(
        workKey = workKey,
        linesAdded = linesAdded,
        linesRemoved = linesRemoved,
        firstCommitDate = firstCommitDate,
        lastCommitDate = lastCommitDate,
        filesChanged = filesChanged.toSet(),
        contributions = absoluteChurnByAuthor
            .map { (author, lines) -> AuthorContribution(author, lines) }
            .sortedByDescending { it.linesChanged },
        absoluteChurnByType = absoluteChurnByType.toMap(),
        commits = commitCount,
        collaborators = collaborators.size,
        reworkFiles = fileTouchCount.filter { it.value > 1 }.keys
    )
}
