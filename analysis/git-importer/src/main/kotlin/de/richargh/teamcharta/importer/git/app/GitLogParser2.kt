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
        val commitType = detectCommitTypes(message)

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
            commitType = commitType
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
            name = match.groups["name"]?.value?.trim() ?: return null,
            email = match.groups["email"]?.value?.trim() ?: return null
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

    private fun detectCommitTypes(message: String): CommitType? {
        val firstWord = firstWordPattern.find(message)?.groups?.get("firstWord")?.value?.lowercase() ?: return null

        return commitTypes[firstWord]
    }

    private val authorPattern = Regex("""(?<name>.+?)\s*<(?<email>[^>]+)>""")
    private val firstWordPattern = Regex("""^\W*(?<firstWord>\w+)""")

    private val commitTypes = mapOf(
        "feat" to CommitType.FEATURE,
        "feature" to CommitType.FEATURE,
        "f" to CommitType.FEATURE,

        "fix" to CommitType.FIX,
        "bug" to CommitType.FIX,
        "bugfix" to CommitType.FIX,
        "hotfix" to CommitType.FIX,
        "b" to CommitType.FIX,

        "refactor" to CommitType.REFACTOR,
        "refactoring" to CommitType.REFACTOR,
        "r" to CommitType.REFACTOR,

        "test" to CommitType.TEST,
        "testing" to CommitType.TEST,
        "t" to CommitType.TEST,

        "build" to CommitType.ENVIRONMENT,
        "chore" to CommitType.ENVIRONMENT,
        "ci" to CommitType.ENVIRONMENT,
        "ops" to CommitType.ENVIRONMENT,
        "e" to CommitType.ENVIRONMENT
    )

}
