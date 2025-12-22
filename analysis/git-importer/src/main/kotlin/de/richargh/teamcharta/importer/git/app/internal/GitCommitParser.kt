package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api.*
import kotlin.collections.map
import kotlin.time.Instant

fun parseCommits(rawCommits: Sequence<RawCommit>): Sequence<Commit> {
    val tracker = BranchTracker()
    return rawCommits.map { parseCommit(it, tracker) }
}

private fun parseCommit(raw: RawCommit, tracker: BranchTracker): Commit {
    val hash = raw.headerFields["hash"]?.let(::CommitHash)
        ?: throw IllegalArgumentException("Commit has no hash")
    val date = raw.headerFields["authorDate"]?.let(Instant::parse)
        ?: throw IllegalArgumentException("Commit has no hash")
    val author = Author(
        name = raw.headerFields["author"] ?: "",
        email = raw.headerFields["authorMail"] ?: ""
    )
    val message = raw.headerFields["subject"] ?: ""
    val parents = parseParents(raw.headerFields["parents"] ?: "")
    val refs = parseRefs(raw.headerFields["refs"] ?: "")

    val fileChanges = parseFileChanges(raw.files.lines())

    val trailers = parseTrailers(raw.trailers)
    val coAuthors = extractCoAuthors(trailers)
    val commitType = detectCommitTypes(message)
    val workKeys = detectWorkKeys(message)

    val tracking = tracker.trackBranch(hash, refs, parents, message)

    return Commit(
        hash = hash,
        author = author,
        date = date,
        message = message,
        parents = parents,
        refs = refs,
        fileChanges = fileChanges,
        trailers = trailers,
        coAuthors = coAuthors,
        commitType = commitType,
        workKeys = workKeys,
        branchId = tracking.branch,
        isOnCurrentBranch = tracking.isOnCurrentBranch
    )
}


private fun parseParents(rawParents: String): List<CommitHash> =
    rawParents.split(" ").filter { it.isNotEmpty() }.map(::CommitHash)


private fun parseRefs(rawRefs: String): List<Ref> {
    if (rawRefs.isEmpty()) return emptyList()
    return rawRefs.split(",").map { it.trim() }.mapNotNull { parseRef(it) }
}

private fun parseRef(rawRef: String): Ref? {
    return when {
        rawRef.startsWith("HEAD -> ") -> Ref.LocalHead(BranchName(rawRef.removePrefix("HEAD -> ")))
        rawRef.startsWith("tag: ") -> Ref.Tag(rawRef.removePrefix("tag: "))
        rawRef.endsWith("/HEAD") -> null // Skip symbolic refs like origin/HEAD
        rawRef.isNotEmpty() -> Ref.BranchTip(rawRef)
        else -> null
    }
}


private fun parseFileChanges(files: List<String>): List<FileChange> = files
    .filter { it.isNotEmpty() }
    .mapNotNull { parseNumstatLine(it) }

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

private fun detectCommitTypes(message: String): CommitType {
    val firstWord = firstWordPattern.find(message)?.groups?.get("firstWord")?.value?.lowercase()
        ?: return CommitType.UNKNOWN

    return commitTypes[firstWord] ?: CommitType.UNKNOWN
}

private fun detectWorkKeys(message: String): List<WorkKey> {
    val workKeys = mutableListOf<WorkKey>()

    gitHubWorkKeyPattern.findAll(message).forEach { match ->
        workKeys.add(WorkKey.Known(match.value))
    }

    jiraWorkKeyPattern.findAll(message).forEach { match ->
        workKeys.add(WorkKey.Known(match.value))
    }

    return workKeys
}

private val authorPattern = Regex("""(?<name>.+?)\s*<(?<email>[^>]+)>""")
private val firstWordPattern = Regex("""^\W*(?<firstWord>\w+)""")

// also used by GitLab, Azure DevOps
private val gitHubWorkKeyPattern = Regex("""#\d+""")

// allegedly also used by TFS, YouTrack, Shortcut
private val jiraWorkKeyPattern = Regex("""\b[A-Z]{2,10}-\d+\b""")

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
    "e" to CommitType.ENVIRONMENT,

    "doc" to CommitType.DOCS,
    "docs" to CommitType.DOCS,
    "documentation" to CommitType.DOCS,
    "d" to CommitType.DOCS,
)