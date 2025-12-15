package de.richargh.teamcharta.importer.git.app.api2

import java.time.ZonedDateTime

class CommitBuilder {
    private var hash: CommitHash = CommitHash("abc123")
    private var author: Author = Author("John Doe", "john@example.com")
    private var date: ZonedDateTime = ZonedDateTime.parse("2024-01-15T10:00:00+01:00")
    private var message: String = "Initial commit"
    private var parents: List<CommitHash> = emptyList()
    private var refs: List<Ref> = emptyList()
    private var fileChanges: List<FileChange> = emptyList()
    private var trailers: List<Pair<String, String>> = emptyList()
    private var coAuthors: Set<Author> = emptySet()
    private var commitType: CommitType = CommitType.UNKNOWN
    private var workKeys: List<WorkKey> = emptyList()
    private var branch: BranchAssignment? = null

    fun hash(hash: String) = apply { this.hash = CommitHash(hash) }
    fun author(name: String, email: String) = apply { this.author = Author(name, email) }
    fun author(author: Author) = apply { this.author = author }
    fun date(date: ZonedDateTime) = apply { this.date = date }
    fun message(message: String) = apply { this.message = message }
    fun parents(vararg parents: String) = apply { this.parents = parents.toList().map(::CommitHash) }
    fun refs(vararg refs: Ref) = apply { this.refs = refs.toList() }
    fun headRef(branchName: String) = apply { this.refs += Ref.Head(BranchName(branchName)) }
    fun branchTip(name: String) = apply { this.refs += Ref.BranchTip(BranchName(name)) }
    fun tag(name: String) = apply { this.refs += Ref.Tag(name) }
    fun fileChanges(vararg fileChanges: FileChange) = apply { this.fileChanges = fileChanges.toList() }
    fun trailers(vararg trailers: Pair<String, String>) = apply { this.trailers = trailers.toList() }
    fun coAuthors(vararg coAuthors: Author) = apply { this.coAuthors = coAuthors.toSet() }
    fun workKeys(vararg workKeys: WorkKey) = apply { this.workKeys = workKeys.toList() }
    fun branch(branch: BranchAssignment?) = apply { this.branch = branch }
    fun certainBranch(name: String) = apply { this.branch = BranchAssignment.Certain(BranchName(name)) }
    fun inferredBranch(name: String) = apply { this.branch = BranchAssignment.Inferred(BranchName(name)) }

    fun build(): Commit = Commit(
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
        branch = branch
    )
}

fun aCommit(block: CommitBuilder.() -> Unit = {}): Commit =
    CommitBuilder().apply(block).build()
