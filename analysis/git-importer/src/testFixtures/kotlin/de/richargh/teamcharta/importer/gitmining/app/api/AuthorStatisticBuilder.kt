package de.richargh.teamcharta.importer.gitmining.app.api

import de.richargh.teamcharta.importer.git.app.api.Author

class AuthorStatisticBuilder {
    private var author: Author = Author("Default Author", "default@example.com")
    private var commitCount: Int = 0
    private var linesAdded: Int = 0
    private var linesRemoved: Int = 0

    fun author(name: String, email: String) = apply { this.author = Author(name, email) }
    fun commitCount(count: Int) = apply { this.commitCount = count }
    fun linesAdded(lines: Int) = apply { this.linesAdded = lines }
    fun linesRemoved(lines: Int) = apply { this.linesRemoved = lines }

    fun build(): AuthorStatistic = AuthorStatistic(
        author = author,
        commitCount = commitCount,
        linesAdded = linesAdded,
        linesRemoved = linesRemoved
    )
}

fun aAuthorStatistic(block: AuthorStatisticBuilder.() -> Unit = {}): AuthorStatistic =
    AuthorStatisticBuilder().apply(block).build()
