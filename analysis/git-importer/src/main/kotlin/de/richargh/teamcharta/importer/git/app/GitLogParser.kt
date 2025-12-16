package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.*
import de.richargh.teamcharta.importer.git.app.internal.splitIntoRawCommits
import de.richargh.teamcharta.importer.git.app.internal.parseCommits

class GitLogParser {

    fun parse(lines: Sequence<String>): List<Commit> {
        val rawCommits = splitIntoRawCommits(lines)
        return parseCommits(rawCommits).toList()
    }

}
