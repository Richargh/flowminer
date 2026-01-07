package de.richargh.flowminer.importer.gitminingfixtures.app.api

import de.richargh.flowminer.importer.git.app.api.Author
import de.richargh.flowminer.importer.git.app.api.CommitType
import de.richargh.flowminer.importer.git.app.api.WorkKey
import de.richargh.flowminer.importer.gitmining.app.api.AuthorStatistic
import de.richargh.flowminer.importer.gitmining.app.api.ChurnMetric

class AuthorStatisticBuilder {
    private var author: Author =
        Author("Default Author", "default@example.com")
    private var commitCount: Int = 0
    private var linesAdded: Int = 0
    private var linesRemoved: Int = 0
    private var workItems: Set<WorkKey> = emptySet()
    private var churnByCommitType: Map<CommitType, ChurnMetric> = emptyMap()
    private var collaborators: Set<Author> = emptySet()

    fun author(name: String, email: String) = apply { this.author =
        Author(name, email)
    }
    fun commitCount(count: Int) = apply { this.commitCount = count }
    fun linesAdded(lines: Int) = apply { this.linesAdded = lines }
    fun linesRemoved(lines: Int) = apply { this.linesRemoved = lines }
    fun workItems(vararg keys: String) = apply {
        this.workItems = keys.map { WorkKey.Known(it) }.toSet()
    }
    fun churn(type: CommitType, additions: Int, deletions: Int) = apply {
        this.churnByCommitType = this.churnByCommitType + (type to ChurnMetric(
            additions,
            deletions
        ))
    }
    fun collaborators(vararg authors: Author) = apply {
        this.collaborators = authors.toSet()
    }

    fun build(): AuthorStatistic =
        AuthorStatistic(
            author = author,
            commitCount = commitCount,
            linesAdded = linesAdded,
            linesRemoved = linesRemoved,
            workItems = workItems,
            churnByCommitType = churnByCommitType,
            collaborators = collaborators
        )
}

fun aAuthorStatistic(block: AuthorStatisticBuilder.() -> Unit = {}): AuthorStatistic =
    AuthorStatisticBuilder().apply(block).build()
