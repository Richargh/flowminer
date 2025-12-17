package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.BranchName
import de.richargh.teamcharta.importer.git.app.api.CommitHash
import de.richargh.teamcharta.importer.git.app.api.hash
import java.time.ZonedDateTime

class GitLogEntryBuilder {
    private var hash: CommitHash = CommitHash("abc123")
    private var author: String = "John Doe"
    private var authorMail: String = "john@example.com"
    private var authorDate: ZonedDateTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00")
    private var subject: String = "Initial commit"
    private var parents: MutableList<CommitHash> = mutableListOf()
    private var refs: List<String> = emptyList()
    private var body: String = ""
    private var trailers: List<Pair<String, String>> = emptyList()
    private var fileChanges: List<FileChangeEntry> = emptyList()

    fun hash(hash: CommitHash) = apply { this.hash = hash }
    fun hash() = hash
    fun author(name: String) = apply { this.author = name }
    fun authorMail(email: String) = apply { this.authorMail = email }
    fun author(name: String, email: String) = apply {
        this.author = name
        this.authorMail = email
    }

    fun authorDate(date: ZonedDateTime) = apply { this.authorDate = date }
    fun subject(subject: String) = apply { this.subject = subject }
    fun parents(vararg parents: CommitHash) = apply { this.parents = parents.toMutableList() }
    fun parents() = parents
    operator fun plus(parent: CommitHash) {
        this.parents.add(0, parent)  // Prepend to match git convention (first parent = target branch)
    }

    fun refs(vararg refs: String) = apply { this.refs = refs.toList() }
    fun refHead(localBranch: String) = apply { this.refs += "HEAD -> $localBranch" }
    fun refBranchTip(branch: BranchName) = apply { this.refs += branch.value }
    fun refOriginHead() = apply { this.refs += "origin/HEAD" }
    fun refTag(name: String) = apply { this.refs += "tag: $name" }
    fun hasHeadRef(): Boolean = refs.any { it.startsWith("HEAD -> ") }
    fun body(body: String) = apply { this.body = body }
    fun trailers(vararg trailers: Pair<String, String>) = apply { this.trailers = trailers.toList() }
    fun fileChanges(vararg changes: FileChangeEntry) = apply { this.fileChanges = changes.toList() }
    fun file(path: String, additions: Int = 0, deletions: Int = 0) = apply {
        this.fileChanges += FileChangeEntry(path, additions, deletions)
    }

    fun build(): String = buildString {
        appendLine("-----COMMIT_START-----")
        // Format: refs|hash|parents|authorDate|author|authorMail|subject
        val parentsStr = parents.joinToString(" ") { it.rawValue }
        val refsStr = refs.joinToString(", ")
        appendLine("$refsStr|${hash.rawValue}|$parentsStr|$authorDate|$author|$authorMail|$subject")
        appendLine("-----BODY_START-----")
        if (body.isNotEmpty()) {
            appendLine(body)
        }
        if (trailers.isNotEmpty()) {
            appendLine("-----TRAILERS_START-----")
            trailers.forEach { (key, value) ->
                appendLine("$key: $value")
            }
        }
        appendLine("-----FILES_START-----")
        fileChanges.forEach { change ->
            appendLine("${change.additions}\t${change.deletions}\t${change.path}")
        }
    }.trimEnd()
}

data class FileChangeEntry(
    val path: String,
    val additions: Int = 0,
    val deletions: Int = 0
)

class GitLogBuilder {
    private val entries = mutableListOf<GitLogEntryBuilder>()
    private val branchForEntry = mutableMapOf<CommitHash, BranchName>()
    private val entriesForBranch = mutableMapOf<BranchName, MutableList<GitLogEntryBuilder>>()
    private val originOfBranch = mutableMapOf<BranchName, BranchName?>()
    private val parentBranchesForEntry = mutableMapOf<CommitHash, List<BranchName>>()
    private val deletedBranches = mutableSetOf<BranchName>()
    // Tracks which branches have been merged into which at which commit index
    // Key: merged branch, Value: (target branch, commit index)
    private val mergedInto = mutableMapOf<BranchName, Pair<BranchName, Int>>()

    fun isDeletedBranch(branch: String) = apply { deletedBranches.add(BranchName(branch)) }

    fun anEntry(branch: String, vararg parentBranches: String, block: GitLogEntryBuilder.() -> Unit = {}): CommitHash {
        val builder = GitLogEntryBuilder()
        builder.hash(entries.size.toString().hash())
        builder.apply(block)

        val branchName = BranchName(branch)
        val parentBranchNames = parentBranches.map { BranchName(it) }

        // First parent branch is used for determining branch origin (fork point)
        if (branchName !in originOfBranch) {
            originOfBranch[branchName] = parentBranchNames.firstOrNull()
        }
        parentBranchesForEntry[builder.hash()] = parentBranchNames

        // Track when a branch is merged INTO this branch (not for first commit / fork)
        val isFirstCommitOnBranch = branchName !in entriesForBranch

        // Add all parent branches as parents
        parentBranchNames.forEach { parentBranch ->
            val parentCommit = findLatestInBranch(parentBranch)
            builder.parents() += parentCommit
            // Only track as merge when a child branch is merged back into its parent
            // e.g., anEntry("origin/main", "origin/feat") means feat is merged into main
            // We only track this if feat originated from main (child merged into parent)
            if (!isFirstCommitOnBranch && parentBranch in entriesForBranch && originOfBranch[parentBranch] == branchName) {
                mergedInto[parentBranch] = branchName to entries.size
            }
        }
        addCommitToBranch(builder, branchName)
        entries.add(builder)

        return builder.hash()
    }

    private fun addCommitToBranch(builder: GitLogEntryBuilder, branch: BranchName) {
        branchForEntry[builder.hash()] = branch
        entriesForBranch.getOrPut(branch) { mutableListOf() }.add(builder)
    }

    fun build(): String = joinEntries()

    private fun joinEntries(): String {
        val result = StringBuilder()
        val reversedEntries = entries.reversed()
        for ((i, e) in reversedEntries.withIndex()) {
            val (branch, before, entry, after) = findBeforeAfterInBranch(e.hash())
            val parentBranches = parentBranchesForEntry[e.hash()] ?: emptyList()
            val currentCommitIndex = entries.size - 1 - i  // Convert reverse index to forward index
            // Add the previous commit on this branch as a parent when:
            // 1. No parent branches specified (continuation on same branch), OR
            // 2. A parent branch originated from this branch (merge feat into main), OR
            // 3. This branch originated from a parent branch (merge main into feat)
            // BUT NOT when this branch was already merged into the parent branch BEFORE this commit (re-branch scenario)
            val wasAlreadyMergedIntoParent = parentBranches.any { parentBranch ->
                val mergeInfo = mergedInto[branch]
                mergeInfo != null && mergeInfo.first == parentBranch && mergeInfo.second < currentCommitIndex
            }
            val shouldAddBefore = before != null && !wasAlreadyMergedIntoParent && (
                parentBranches.isEmpty() ||
                parentBranches.any { originOfBranch[it] == branch || originOfBranch[branch] == it }
            )
            if (shouldAddBefore)
                entry + before!!.hash()
            if (after == null && branch !in deletedBranches) {
                entry.refBranchTip(branch)
                // Add origin/HEAD when this is the HEAD commit and branch is origin/*
                if (entry.hasHeadRef() && branch.value.startsWith("origin/")) {
                    entry.refOriginHead()
                }
            }
            result.append(entry.build())

            if (i < reversedEntries.size) {
                result.append("\n")
            }
        }
        return result.toString()
    }

    private fun findLatestInBranch(
        branch: BranchName
    ): CommitHash {
        val allInBranch = entriesForBranch[branch]
        return allInBranch!!.last().hash()
    }

    private fun findBeforeAfterInBranch(
        hash: CommitHash,
    ): FindGitLogResult {
        val branch = branchForEntry[hash]!!
        val allInBranch = entriesForBranch[branch]
        val indexInBranch = allInBranch?.indexOfFirst { it.hash() == hash }!!
        return FindGitLogResult(
            branch,
            allInBranch.getOrNull(indexInBranch - 1),
            allInBranch.getOrNull(indexInBranch)!!,
            allInBranch.getOrNull(indexInBranch + 1)
        )
    }

    private data class FindGitLogResult(
        val branch: BranchName,
        val before: GitLogEntryBuilder?,
        val current: GitLogEntryBuilder,
        val after: GitLogEntryBuilder?
    )
}

fun aGitLog(block: GitLogBuilder.() -> Unit = {}): String =
    GitLogBuilder().apply(block).build()

