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
        val commitTypes = detectCommitTypes(message)

        return Commit(
            hash = hash,
            author = Author(authorName, authorEmail),
            date = date,
            message = message,
            parents = parents,
            refs = refs,
            fileChanges = fileChanges,
            trailers = trailers,
            coAuthors = coAuthors,
            commitTypes = commitTypes
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

    private fun extractCoAuthors(trailers: List<Pair<String, String>>): Set<Author> {
        return trailers
            .filter { it.first.equals("Co-authored-by", ignoreCase = true) }
            .mapNotNull { parseAuthorValue(it.second) }
            .toSet()
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

    private fun detectCommitTypes(message: String): List<CommitType> {
        val types = mutableListOf<CommitType>()
        if (featurePattern.containsMatchIn(message)) {
            types.add(CommitType.FEATURE)
        }
        if (fixPattern.containsMatchIn(message)) {
            types.add(CommitType.FIX)
        }
        if (refactorPattern.containsMatchIn(message)) {
            types.add(CommitType.REFACTOR)
        }
        if (testPattern.containsMatchIn(message)) {
            types.add(CommitType.TEST)
        }
        return types
    }

    private val authorPattern = Regex("""(.+?)\s*<([^>]+)>""")
    private val featurePattern = Regex("""^\s*([.^@!]\s+)?(feat|feature|f)\s*(\(.+\)|\[.+\])?\s*:?\s""", RegexOption.IGNORE_CASE)
    private val fixPattern = Regex("""^\s*([.^@!]\s+)?(fix|bug|bugfix|hotfix|b)\s*(\(.+\)|\[.+\])?\s*:?\s""", RegexOption.IGNORE_CASE)
    private val refactorPattern = Regex("""^\s*([.^@!]\s+)?(refactor|refactoring|r)\s*(\(.+\)|\[.+\])?\s*:?\s""", RegexOption.IGNORE_CASE)
    private val testPattern = Regex("""^\s*([.^@!]\s+)?(test|testing|t)\s*(\(.+\)|\[.+\])?\s*:?\s""", RegexOption.IGNORE_CASE)

}
