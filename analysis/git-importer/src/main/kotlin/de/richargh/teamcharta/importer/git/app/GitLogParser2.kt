package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.*
import de.richargh.teamcharta.importer.git.app.internal.splitIntoRawCommits
import de.richargh.teamcharta.importer.git.app.internal.RawCommit
import java.time.ZonedDateTime

class GitLogParser2 {

    fun parse(lines: Sequence<String>): GitMiningResult {
        val rawCommits = splitIntoRawCommits(lines)
        val commits = rawCommits.map { parseCommit(it) }
        return GitMiningResult(commits = commits)
    }

    private fun parseCommit(raw: RawCommit): Commit {
        val hash = raw.headerFields["hash"] ?: ""
        val authorName = raw.headerFields["author"] ?: ""
        val authorEmail = raw.headerFields["authorMail"] ?: ""
        val rawDate = raw.headerFields["authorDate"] ?: ""
        val message = raw.headerFields["subject"] ?: ""
        val parentsStr = raw.headerFields["parents"] ?: ""
        val refsStr = raw.headerFields["refs"] ?: ""

        val date = ZonedDateTime.parse(rawDate)
        val parents = parentsStr.split(" ").filter { it.isNotEmpty() }
        val refs = if (refsStr.isEmpty()) emptyList() else listOf(refsStr)

        val fileChanges = raw.files.lines()
            .filter { it.isNotEmpty() }
            .mapNotNull { parseNumstatLine(it) }

        val trailers = parseTrailers(raw.trailers)
        val coAuthors = extractCoAuthors(trailers)

        return Commit(
            hash = hash,
            author = Author(authorName, authorEmail),
            date = date,
            message = message,
            parents = parents,
            refs = refs,
            fileChanges = fileChanges,
            trailers = trailers,
            coAuthors = coAuthors
        )
    }

    private fun parseTrailers(trailersStr: String): List<Pair<String, String>> {
        if (trailersStr.isBlank()) return emptyList()
        return trailersStr.lines()
            .filter { it.contains(":") }
            .map { line ->
                val key = line.substringBefore(":").trim()
                val value = line.substringAfter(":").trim()
                key to value
            }
    }

    private fun extractCoAuthors(trailers: List<Pair<String, String>>): List<Author> {
        return trailers
            .filter { it.first.equals("Co-authored-by", ignoreCase = true) }
            .mapNotNull { parseAuthorValue(it.second) }
    }

    private fun parseAuthorValue(value: String): Author? {
        val match = authorPattern.matchEntire(value) ?: return null
        return Author(
            name = match.groupValues[1].trim(),
            email = match.groupValues[2].trim()
        )
    }

    private fun parseNumstatLine(line: String): FileChange? {
        val parts = line.split("\t")
        if (parts.size < 3) return null

        val additions = parts[0].toIntOrNull() ?: 0
        val deletions = parts[1].toIntOrNull() ?: 0
        val path = parts[2]

        return FileChange(
            path = path,
            additions = additions,
            deletions = deletions
        )
    }

    private val authorPattern = Regex("""(.+?)\s*<([^>]+)>""")

}
