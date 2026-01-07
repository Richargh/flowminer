package de.richargh.flowminer.importer.gitcli

import de.richargh.flowminer.importer.git.app.api.Commit
import de.richargh.flowminer.importer.git.app.api.toDto
import kotlinx.serialization.json.Json

object JsonlFormatter {

    private val json = Json { encodeDefaults = true }

    fun format(commits: Sequence<Commit>): Sequence<String> {
        return commits.map { commit ->
            json.encodeToString(commit.toDto())
        }
    }
}
