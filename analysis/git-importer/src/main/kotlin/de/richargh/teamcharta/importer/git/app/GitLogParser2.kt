package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.*
import de.richargh.teamcharta.importer.git.app.internal.splitIntoRawCommits
import de.richargh.teamcharta.importer.git.app.internal.extractBranchInfo
import de.richargh.teamcharta.importer.git.app.internal.parseCommits

class GitLogParser2 {

    fun parse(lines: Sequence<String>): GitMiningResult {
        val rawCommits = splitIntoRawCommits(lines)
        val commits = parseCommits(rawCommits).toList()
        val branches = extractBranchInfo(commits)
        return GitMiningResult(commits = Commits(commits), branches = branches)
    }

}
