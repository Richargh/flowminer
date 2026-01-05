package de.richargh.teamcharta.importer.githubcli

import de.richargh.teamcharta.importer.github.app.api.GitHubWorkItem
import de.richargh.teamcharta.importer.github.app.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

object JsonlFormatter {

    private val json = Json { encodeDefaults = true }

    fun format(workItems: Flow<GitHubWorkItem>): Flow<String> {
        return workItems.map { workItem ->
            json.encodeToString(workItem.toDto())
        }
    }
}
