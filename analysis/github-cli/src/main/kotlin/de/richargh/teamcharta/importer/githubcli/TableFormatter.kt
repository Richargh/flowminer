package de.richargh.teamcharta.importer.githubcli

import de.richargh.teamcharta.importer.github.app.api.GitHubWorkItem

object TableFormatter {

    fun format(workItems: List<GitHubWorkItem>): Sequence<String> = sequence {
        if (workItems.isEmpty()) {
            yield("No issues found.")
            return@sequence
        }

        // Column headers
        val headers = listOf("ID", "Title", "State", "Type", "Labels", "Assignees", "Milestone")

        // Calculate column widths
        val rows = workItems.map { item ->
            listOf(
                "#${item.id.value}",
                item.title.take(40) + if (item.title.length > 40) "..." else "",
                item.state.name,
                item.type?.name ?: "-",
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
        yield(headerLine)
        yield("-".repeat(headerLine.length))

        // Format rows
        for (row in rows) {
            yield(row.mapIndexed { i, cell -> cell.padEnd(widths[i]) }.joinToString(" | "))
        }

        yield("")
        yield("Total: ${workItems.size} issues")
    }
}
