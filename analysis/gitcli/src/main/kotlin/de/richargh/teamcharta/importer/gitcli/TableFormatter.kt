package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.gitmining.app.api.Branch
import java.time.format.DateTimeFormatter

object TableFormatter {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun formatBranches(branches: List<Branch>): String {
        val headers = listOf("Name", "First Commit", "Last Commit", "Merged", "Status")
        val rows = branches.map { branch ->
            listOf(
                branch.name?.toString() ?: "(unnamed)",
                branch.firstCommitDate.format(dateFormatter),
                branch.lastCommitDate.format(dateFormatter),
                if (branch.mergeCommitHash != null) "Yes" else "No",
                if (branch.isActive) "Active" else "Stale"
            )
        }
        return formatTable(headers, rows)
    }

    fun formatCommits(commits: List<Commit>): String {
        val headers = listOf("Hash", "Date", "Author", "Message")
        val rows = commits.map { commit ->
            listOf(
                commit.hash.rawValue.take(7),
                commit.date.format(dateFormatter),
                commit.author.name,
                commit.message
            )
        }
        return formatTable(headers, rows)
    }

    private fun formatTable(headers: List<String>, rows: List<List<String>>): String {
        val columnWidths = calculateColumnWidths(headers, rows)

        val headerRow = formatRow(headers, columnWidths)
        val separatorRow = columnWidths.joinToString("|", "|", "|") { "-".repeat(it + 2) }
        val dataRows = rows.map { formatRow(it, columnWidths) }

        return (listOf(headerRow, separatorRow) + dataRows).joinToString("\n")
    }

    private fun calculateColumnWidths(headers: List<String>, rows: List<List<String>>): List<Int> {
        return headers.indices.map { col ->
            val headerWidth = headers[col].length
            val maxDataWidth = rows.maxOfOrNull { it[col].length } ?: 0
            maxOf(headerWidth, maxDataWidth)
        }
    }

    private fun formatRow(values: List<String>, columnWidths: List<Int>): String {
        return values.mapIndexed { index, value ->
            value.padEnd(columnWidths[index])
        }.joinToString(" | ", "| ", " |")
    }
}
