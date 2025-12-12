package de.richargh.teamcharta.importer.git.app

class GitLogEntryBuilder {
    private var hash: String = "abc123"
    private var author: String = "John Doe"
    private var authorMail: String = "john@example.com"
    private var authorDate: String = "2024-01-15T10:00:00+01:00"
    private var subject: String = "Initial commit"
    private var parents: List<String> = emptyList()
    private var refs: List<String> = emptyList()
    private var body: String = ""
    private var trailers: List<Pair<String, String>> = emptyList()
    private var fileChanges: List<FileChangeEntry> = emptyList()

    fun hash(hash: String) = apply { this.hash = hash }
    fun author(name: String) = apply { this.author = name }
    fun authorMail(email: String) = apply { this.authorMail = email }
    fun author(name: String, email: String) = apply {
        this.author = name
        this.authorMail = email
    }
    fun authorDate(date: String) = apply { this.authorDate = date }
    fun subject(subject: String) = apply { this.subject = subject }
    fun parents(vararg parents: String) = apply { this.parents = parents.toList() }
    fun refs(vararg refs: String) = apply { this.refs = refs.toList() }
    fun headRef(branchName: String) = apply { this.refs = this.refs + "HEAD -> $branchName" }
    fun branch(name: String) = apply { this.refs = this.refs + name }
    fun tag(name: String) = apply { this.refs = this.refs + "tag: $name" }
    fun body(body: String) = apply { this.body = body }
    fun trailers(vararg trailers: Pair<String, String>) = apply { this.trailers = trailers.toList() }
    fun fileChanges(vararg changes: FileChangeEntry) = apply { this.fileChanges = changes.toList() }
    fun file(path: String, additions: Int = 0, deletions: Int = 0) = apply {
        this.fileChanges = this.fileChanges + FileChangeEntry(path, additions, deletions)
    }

    fun build(): String = buildString {
        appendLine("-----COMMIT_START-----")
        appendLine("hash==>> $hash")
        appendLine("author==>> $author")
        appendLine("authorMail==>> $authorMail")
        appendLine("authorDate==>> $authorDate")
        appendLine("subject==>> $subject")
        appendLine("parents==>> ${parents.joinToString(" ")}")
        appendLine("refs==>> ${refs.joinToString(", ")}")
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

    fun anEntry(block: GitLogEntryBuilder.() -> Unit = {}) = apply {
        entries.add(GitLogEntryBuilder().apply(block))
    }

    fun build(): String = entries.joinToString("\n") { it.build() }
}

fun aGitLog(block: GitLogBuilder.() -> Unit = {}): String =
    GitLogBuilder().apply(block).build()

fun anEntry(block: GitLogEntryBuilder.() -> Unit = {}): String =
    GitLogEntryBuilder().apply(block).build()
