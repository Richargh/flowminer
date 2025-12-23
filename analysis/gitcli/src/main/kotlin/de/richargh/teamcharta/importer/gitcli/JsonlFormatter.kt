package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.git.app.api.toDto
import kotlinx.serialization.json.Json

object JsonlFormatter {

    private val json = Json { encodeDefaults = true }

    fun format(commits: List<Commit>): String {
        if (commits.isEmpty()) return ""
        return commits.joinToString("\n") { commit ->
            json.encodeToString(commit.toDto())
        }
    }
}
