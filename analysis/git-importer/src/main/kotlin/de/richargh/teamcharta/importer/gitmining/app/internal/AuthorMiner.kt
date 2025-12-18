package de.richargh.teamcharta.importer.gitmining.app.internal

import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.gitmining.app.api.AuthorStatistic
import de.richargh.teamcharta.importer.gitmining.app.api.AuthorStatistics

fun extractAuthorStatistics(commits: List<Commit>): AuthorStatistics {
    val authorStatistics = commits
        .groupBy { it.author }
        .map { (author, authorCommits) ->
            AuthorStatistic(
                author = author,
                commitCount = authorCommits.size,
                linesAdded = authorCommits.sumOf { commit ->
                    commit.fileChanges.sumOf { it.additions }
                },
                linesRemoved = authorCommits.sumOf { commit ->
                    commit.fileChanges.sumOf { it.deletions }
                }
            )
        }
    return AuthorStatistics(authorStatistics)
}
