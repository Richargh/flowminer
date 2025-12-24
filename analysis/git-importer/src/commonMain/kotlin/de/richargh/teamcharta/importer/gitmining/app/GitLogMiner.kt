package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.git.app.internal.parseCommits
import de.richargh.teamcharta.importer.git.app.internal.splitIntoRawCommits
import de.richargh.teamcharta.importer.gitmining.app.api.Commits
import de.richargh.teamcharta.importer.gitmining.app.api.GitMiningResult
import de.richargh.teamcharta.importer.gitmining.app.internal.extractAuthorStatistics
import de.richargh.teamcharta.importer.gitmining.app.internal.extractBranchInfo
import de.richargh.teamcharta.importer.gitmining.app.internal.extractWorkItems
import kotlin.time.Instant

class GitLogMiner {

    fun mine(c: Sequence<Commit>, currentDate: Instant): GitMiningResult {
        val commits = c.toList()
        val branches = extractBranchInfo(commits, currentDate)
        val workItems = extractWorkItems(commits)
        val authorStatistics = extractAuthorStatistics(commits)
        return GitMiningResult(commits = Commits(commits), branches = branches, workItems = workItems, authorStatistics = authorStatistics)
    }

}
