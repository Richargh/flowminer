package de.richargh.teamcharta.importer.githubcli

import de.richargh.teamcharta.importer.github.app.api.GitHubWorkItem
import de.richargh.teamcharta.importer.github.app.toDto
import kotlinx.serialization.json.Json

object JsonlFormatter {

    private val json = Json { encodeDefaults = true }

    fun format(workItems: List<GitHubWorkItem>): Sequence<String> {
        return workItems.asSequence().map { workItem ->
            json.encodeToString(workItem.toDto())
        }
    }
}
