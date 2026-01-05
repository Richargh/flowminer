package de.richargh.teamcharta.importer.githubcli

import de.richargh.teamcharta.importer.github.app.api.GitHubWorkItem
import de.richargh.teamcharta.importer.github.app.api.TransitionField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

object TableFormatter {

    suspend fun format(workItems: Flow<GitHubWorkItem>, limit: Int = 20): Flow<String> {
        val items = workItems.take(limit).toList()

        return flow {
            if (items.isEmpty()) {
                emit("No issues found.")
                return@flow
            }

            // Column headers
            val headers = listOf("ID", "Title", "State", "Type", "Transitions", "Labels", "Assignees", "Milestone")

            // Calculate column widths
            val rows = items.map { item ->
                listOf(
                    "#${item.id.value}",
                    item.title.take(40) + if (item.title.length > 40) "..." else "",
                    item.state.name,
                    item.type?.name ?: "-",
                    formatTransitions(item),
                    item.labels.take(2).joinToString(", ").take(20) + if (item.labels.size > 2) "..." else "",
                    item.assignees.take(2).joinToString(", ").take(20) + if (item.assignees.size > 2) "..." else "",
                    item.milestone ?: "-"
                )
            }

            val widths = headers.indices.map { i ->
                maxOf(headers[i].length, rows.maxOfOrNull { it[i].length } ?: 0)
            }

            // Format header
            val headerLine = headers.mapIndexed { i, h -> h.padEnd(widths[i]) }.joinToString(" | ")
            emit(headerLine)
            emit("-".repeat(headerLine.length))

            // Format rows
            for (row in rows) {
                emit(row.mapIndexed { i, cell -> cell.padEnd(widths[i]) }.joinToString(" | "))
            }

            emit("")
            emit("Total: ${items.size} issues" + if (items.size == limit) " (limited)" else "")
        }
    }

    private fun formatTransitions(item: GitHubWorkItem): String {
        val transitions = item.transitions
        if (transitions.isEmpty()) return "-"

        // Group by field and show summary
        val summary = transitions
//            .filter { it.field == TransitionField.Milestone || it.field == TransitionField.Assignee }
            .map { "${it.field}[${it.from} -> ${it.to}]" }
            .take(3)
            .joinToString(", ")

        return if (summary.length > 50) summary.take(47) + "..." else summary
    }
}
