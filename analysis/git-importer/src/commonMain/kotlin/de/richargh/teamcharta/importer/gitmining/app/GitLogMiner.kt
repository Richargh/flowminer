package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.internal.splitIntoRawCommits
import de.richargh.teamcharta.importer.gitmining.app.internal.extractAuthorStatistics
import de.richargh.teamcharta.importer.gitmining.app.internal.extractBranchInfo
import de.richargh.teamcharta.importer.gitmining.app.internal.extractWorkItems
import de.richargh.teamcharta.importer.git.app.internal.parseCommits
import de.richargh.teamcharta.importer.gitmining.app.api.Commits
import de.richargh.teamcharta.importer.gitmining.app.api.GitMiningResult
import kotlin.time.Instant

class GitLogMiner {

    fun parse(lines: Sequence<String>, currentDate: Instant): GitMiningResult {
        val rawCommits = splitIntoRawCommits(lines)
        val commits = parseCommits(rawCommits).toList()
        val branches = extractBranchInfo(commits, currentDate)
        val workItems = extractWorkItems(commits)
        val authorStatistics = extractAuthorStatistics(commits)
        return GitMiningResult(commits = Commits(commits), branches = branches, workItems = workItems, authorStatistics = authorStatistics)
    }

}
