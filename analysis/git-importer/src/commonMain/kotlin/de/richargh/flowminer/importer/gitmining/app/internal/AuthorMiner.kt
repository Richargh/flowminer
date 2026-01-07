package de.richargh.flowminer.importer.gitmining.app.internal

import de.richargh.flowminer.importer.git.app.api.Author
import de.richargh.flowminer.importer.git.app.api.Commit
import de.richargh.flowminer.importer.git.app.api.WorkKey
import de.richargh.flowminer.importer.gitmining.app.api.AuthorStatistic
import de.richargh.flowminer.importer.gitmining.app.api.AuthorStatistics
import de.richargh.flowminer.importer.gitmining.app.api.ChurnMetric

fun extractAuthorStatistics(commits: List<Commit>): AuthorStatistics {
    // Build work item to authors mapping
    val workItemToAuthors: Map<WorkKey, Set<Author>> = commits
        .flatMap { commit -> commit.workKeys.map { workKey -> workKey to commit.author } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, authors) -> authors.toSet() }

    val authorStatistics = commits
        .groupBy { it.author }
        .map { (author, authorCommits) ->
            // Find collaborators from shared work items
            val authorWorkItems = authorCommits.flatMap { it.workKeys }.toSet()
            val workItemCollaborators = authorWorkItems
                .flatMap { workKey -> workItemToAuthors[workKey] ?: emptySet() }
                .filter { it != author }
                .toSet()

            // Combine co-authors and work item collaborators
            val directCoAuthors = authorCommits.flatMap { it.coAuthors }.toSet()

            AuthorStatistic(
                author = author,
                commitCount = authorCommits.size,
                linesAdded = authorCommits.sumOf { commit ->
                    commit.fileChanges.sumOf { it.additions }
                },
                linesRemoved = authorCommits.sumOf { commit ->
                    commit.fileChanges.sumOf { it.deletions }
                },
                workItems = authorWorkItems,
                churnByCommitType = authorCommits
                    .groupBy { it.commitType }
                    .mapValues { (_, commits) ->
                        commits.fold(
                            ChurnMetric(
                                0,
                                0
                            )
                        ) { acc, commit ->
                            val additions = commit.fileChanges.sumOf { it.additions }
                            val deletions = commit.fileChanges.sumOf { it.deletions }
                            acc + ChurnMetric(
                                additions,
                                deletions
                            )
                        }
                    },
                collaborators = directCoAuthors + workItemCollaborators
            )
        }
    return AuthorStatistics(authorStatistics)
}
