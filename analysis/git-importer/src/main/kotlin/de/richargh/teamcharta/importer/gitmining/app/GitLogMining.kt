package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.internal.splitIntoRawCommits
import de.richargh.teamcharta.importer.gitmining.app.internal.extractBranchInfo
import de.richargh.teamcharta.importer.git.app.internal.parseCommits
import de.richargh.teamcharta.importer.gitmining.app.api.Commits
import de.richargh.teamcharta.importer.gitmining.app.api.GitMiningResult

class GitLogMining {

    fun parse(lines: Sequence<String>): GitMiningResult {
        val rawCommits = splitIntoRawCommits(lines)
        val commits = parseCommits(rawCommits).toList()
        val branches = extractBranchInfo(commits)
        return GitMiningResult(commits = Commits(commits), branches = branches)
    }

}
