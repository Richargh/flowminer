package de.richargh.flowminer.importer.git.app

import de.richargh.flowminer.importer.git.app.api.*
import de.richargh.flowminer.importer.git.app.internal.splitIntoRawCommits
import de.richargh.flowminer.importer.git.app.internal.parseCommits

class GitLogParser {

    fun parse(lines: Sequence<String>): Sequence<Commit> {
        val rawCommits = splitIntoRawCommits(lines)
        return parseCommits(rawCommits)
    }

}
