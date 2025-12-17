package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.git.app.internal.splitIntoRawCommits
import de.richargh.teamcharta.importer.gitmining.app.internal.extractBranchInfo
import de.richargh.teamcharta.importer.git.app.internal.parseCommits
import de.richargh.teamcharta.importer.gitmining.app.api.Commits
import de.richargh.teamcharta.importer.gitmining.app.api.GitMiningResult
import java.time.ZonedDateTime

class GitLogMiner {

    fun parse(lines: Sequence<String>, currentDate: ZonedDateTime): GitMiningResult {
        val rawCommits = splitIntoRawCommits(lines)
        val commits = parseCommits(rawCommits).toList()
        val branches = extractBranchInfo(commits, currentDate)
        return GitMiningResult(commits = Commits(commits), branches = branches)
    }

}
