package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.BranchName
import de.richargh.teamcharta.importer.git.app.api2.CommitHash
import de.richargh.teamcharta.importer.git.app.api2.hash
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
    fun refHead(branch: String) = apply { this.refs += "HEAD -> $branch" }
    fun refBranchTip(branch: BranchName) = apply { this.refs += branch.value }
    fun refTag(name: String) = apply { this.refs += "tag: $name" }
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
    private val parentBranchForEntry = mutableMapOf<CommitHash, BranchName?>()

    fun anEntry(branch: String, parentBranchName: String? = null, block: GitLogEntryBuilder.() -> Unit = {}): CommitHash {
        val builder = GitLogEntryBuilder()
        builder.hash(entries.size.toString().hash())
        builder.apply(block)

        val branchName = BranchName(branch)
        if (branchName !in originOfBranch) {
            originOfBranch[branchName] = parentBranchName?.let { BranchName(it) }
        }
        parentBranchForEntry[builder.hash()] = parentBranchName?.let { BranchName(it) }

        if(parentBranchName != null) {
            val parentCommit = findLatestInBranch(BranchName(parentBranchName))
            builder.parents(parentCommit)
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
            val parentBranch = parentBranchForEntry[e.hash()]
            val shouldAddBefore = before != null && (
                parentBranch == null ||
                originOfBranch[parentBranch] == branch
            )
            if (shouldAddBefore)
                entry + before!!.hash()
            if (after == null)
                entry.refBranchTip(branch)
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

