package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.gitmining.app.api.Branch
import de.richargh.teamcharta.importer.gitmining.app.api.BranchStatus
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TableFormatter {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private const val STALE_LIMIT = 10
    private const val COMPLETED_LIMIT = 10

    fun formatBranches(branches: List<Branch>, now: ZonedDateTime): String {
        val active = branches.filter { it.status == BranchStatus.Active }
            .sortedByDescending { it.lastCommitDate }
        val stale = branches.filter { it.status == BranchStatus.Stale }
            .sortedBy { it.lastCommitDate }
        val completed = branches.filter { it.status == BranchStatus.Completed }
            .sortedByDescending { it.lastCommitDate }

        val sections = mutableListOf<String>()

        if (active.isNotEmpty()) {
            sections.add(formatBranchSection("Active Branches", active, now, limit = null))
        } else {
            sections.add("No Active Branches")
        }

        if (stale.isNotEmpty()) {
            sections.add(formatBranchSection("Stale Branches", stale, now, limit = STALE_LIMIT, hiddenLabel = "stale"))
        } else {
            sections.add("No Stale Branches")
        }

        if (completed.isNotEmpty()) {
            sections.add(formatBranchSection("Completed Branches", completed, now, limit = COMPLETED_LIMIT, hiddenLabel = "completed"))
        } else {
            sections.add("No Completed Branches")
        }

        return sections.joinToString("\n\n")
    }

    private fun formatBranchSection(
        title: String,
        branches: List<Branch>,
        now: ZonedDateTime,
        limit: Int?,
        hiddenLabel: String? = null
    ): String {
        val displayBranches = if (limit != null) branches.take(limit) else branches
        val hiddenCount = if (limit != null) maxOf(0, branches.size - limit) else 0

        val headers = listOf("Name", "Updated", "Age")
        val rows = displayBranches.map { branch ->
            listOf(
                formatBranchName(branch),
                formatRelativeDate(branch.lastCommitDate, now),
                formatAge(branch.firstCommitDate.toLocalDate(), branch.lastCommitDate.toLocalDate())
            )
        }

        val table = formatTable(headers, rows)
        val titleWithCount = "$title (${displayBranches.size} out of ${branches.size})"

        return if (hiddenCount > 0 && hiddenLabel != null) {
            "$titleWithCount\n$table\n... $hiddenCount more $hiddenLabel branches not shown"
        } else {
            "$titleWithCount\n$table"
        }
    }

    private fun formatBranchName(branch: Branch): String {
        val prefix = if (branch.isCurrent) "* " else "  "
        return prefix + (branch.name?.toString() ?: "(unnamed)")
    }

    private fun formatRelativeDate(date: ZonedDateTime, now: ZonedDateTime): String {
        val period = Period.between(date.toLocalDate(), now.toLocalDate())
        return formatPeriod(period) + " ago"
    }

    private fun formatAge(from: LocalDate, to: LocalDate): String {
        val period = Period.between(from, to)
        return formatPeriod(period)
    }

    private fun formatPeriod(period: Period): String {
        return when {
            period.years > 0 -> "${period.years} years"
            period.months > 0 -> "${period.months} months"
            period.days >= 7 -> "${period.days / 7} weeks"
            else -> "${period.days} days"
        }
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
