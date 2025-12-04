package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.*
import de.richargh.teamcharta.importer.git.app.internal.GitLogEntry
import java.time.Instant

class GitLogParser {

    fun parseLog(gitLogOutput: String, defaultBranch: String = "main"): GitEventSeries {
        val logEntries = parseGitLogOutput(gitLogOutput)
        val events = extractEvents(logEntries, defaultBranch)

        return GitEventSeries(
            events = events.sortedBy { it.timestamp },
            defaultBranch = defaultBranch
        )
    }

    private fun parseGitLogOutput(output: String): List<GitLogEntry> {
        return output.lines().mapNotNull { tryParseLine(it) }
    }

    private fun tryParseLine(line: String): GitLogEntry? {
        if (line.isEmpty()) {
            return null
        }
        val commitData = tryParseCommitLine(line)
            ?: return null

        return GitLogEntry(
            hash = commitData.hash,
            parents = commitData.parents,
            author = commitData.author,
            timestamp = commitData.timestamp,
            refs = commitData.refs,
            message = commitData.message,
        )
    }

    private fun tryParseCommitLine(commitLine: String): CommitData? {
        val parts = commitLine.split("|")
        if (parts.size < 6) return null

        return CommitData(
            hash = parts[0],
            parents = parseParents(parts[1]),
            author = parts[2],
            timestamp = Instant.parse(parts[3]),
            refs = parseRefs(parts[4]),
            message = parts[5]
        )
    }

    private fun parseParents(parentString: String): List<String> =
        parentString.split(" ").filter { it.isNotEmpty() }

    private fun parseRefs(refString: String): List<String> =
        refString.split(",").map { it.trim() }.filter { it.isNotEmpty() }


    private data class CommitData(
        val hash: String,
        val parents: List<String>,
        val author: String,
        val timestamp: Instant,
        val refs: List<String>,
        val message: String
    )

    private fun extractEvents(logEntries: List<GitLogEntry>, defaultBranch: String): List<GitEvent> {
        val events = mutableListOf<GitEvent>()
        val branchFirstCommits = mutableMapOf<String, GitLogEntry>()
        val commitToBranch = mutableMapOf<String, String>()

        // Build commit-to-branch mapping from refs
        for (entry in logEntries) {
            for (ref in entry.refs) {
                val branchName = extractBranchName(ref)
                if (branchName != null) {
                    commitToBranch[entry.hash] = branchName

                    // Track first commit for each branch
                    if (!branchFirstCommits.containsKey(branchName)) {
                        branchFirstCommits[branchName] = entry
                    }
                }
            }
        }

        // Process each commit
        for (entry in logEntries) {
            val branchName = inferBranchName(entry, commitToBranch, defaultBranch)

            // Check if this is a merge commit
            if (entry.parents.size > 1) {
                // This is a merge commit
                val mergedBranch = extractMergedBranchFromMessage(entry.message) ?: "unknown"
                events.add(
                    BranchMerged(
                        timestamp = entry.timestamp,
                        commitHash = entry.hash,
                        branchName = mergedBranch,
                        targetBranch = branchName,
                        author = entry.author,
                        mergeMessage = entry.message
                    )
                )
            }

            // Add commit event
            events.add(
                CommitMade(
                    timestamp = entry.timestamp,
                    commitHash = entry.hash,
                    branchName = branchName,
                    author = entry.author,
                    message = entry.message,
                )
            )

            // Check if this is the first commit on a new branch
            if (branchFirstCommits[branchName]?.hash == entry.hash && branchName != defaultBranch) {
                val parentBranch = inferParentBranch(entry, commitToBranch, defaultBranch)
                events.add(
                    BranchCreated(
                        timestamp = entry.timestamp,
                        commitHash = entry.hash,
                        branchName = branchName,
                        parentBranch = parentBranch,
                        author = entry.author
                    )
                )
            }
        }

        return events
    }

    private fun extractBranchName(ref: String): String? {
        // Handle different ref formats
        return when {
            ref.startsWith("HEAD -> ") -> ref.removePrefix("HEAD -> ")
            ref.startsWith("origin/") -> ref.removePrefix("origin/")
            ref.contains("tag: ") -> null // Ignore tags
            ref.contains("/") -> ref.substringAfterLast("/")
            else -> ref
        }
    }

    private fun inferBranchName(
        entry: GitLogEntry,
        commitToBranch: Map<String, String>,
        defaultBranch: String
    ): String {
        // First check if commit is directly tagged with a branch
        commitToBranch[entry.hash]?.let { return it }

        // Check refs
        for (ref in entry.refs) {
            extractBranchName(ref)?.let { return it }
        }

        // Default to main/master branch
        return defaultBranch
    }

    private fun inferParentBranch(
        entry: GitLogEntry,
        commitToBranch: Map<String, String>,
        defaultBranch: String
    ): String? {
        // Check the first parent commit's branch
        if (entry.parents.isNotEmpty()) {
            return commitToBranch[entry.parents[0]] ?: defaultBranch
        }
        return null
    }

    private fun extractMergedBranchFromMessage(message: String): String? {
        // Try to extract branch name from common merge message patterns
        // "Merge branch 'feature-x' into main"
        val mergePattern = Regex("Merge branch '([^']+)'")
        val match = mergePattern.find(message)
        return match?.groupValues?.getOrNull(1)
    }
}
