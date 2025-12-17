package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.git.app.api.Commit
import de.richargh.teamcharta.importer.git.app.api.CommitType
import de.richargh.teamcharta.importer.gitmining.app.api.Branch
import de.richargh.teamcharta.importer.gitmining.app.api.BranchStatus
import de.richargh.teamcharta.importer.gitmining.app.api.WorkItem
import java.time.Duration
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
                formatAge(branch.firstCommitDate.toLocalDate(), now.toLocalDate())
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

    fun formatCommits(commits: List<Commit>, totalCount: Int = commits.size): String {
        val headers = listOf("Branch", "Date", "Type", "Author", "Workkeys", "+", "-", "Message")
        var headMarked = false
        val rows = commits.map { commit ->
            val isHead = commit.isOnCurrentBranch && !headMarked
            if (isHead) headMarked = true
            listOf(
                formatCommitBranch(commit, isHead),
                commit.date.format(dateFormatter),
                commit.commitType.name,
                commit.author.name,
                commit.workKeys.joinToString(",") { it.toString() },
                commit.fileChanges.sumOf { it.additions }.toString(),
                commit.fileChanges.sumOf { it.deletions }.toString(),
                commit.message
            )
        }
        val table = formatTable(headers, rows)
        val header = "Commits (${commits.size} out of $totalCount)"
        return "$header\n$table"
    }

    private fun formatCommitBranch(commit: Commit, isHead: Boolean): String {
        val prefix = if (isHead) "* " else "  "
        return prefix + (commit.branch.name?.toString() ?: "-")
    }

    fun formatWorkItems(workItems: List<WorkItem>, totalCount: Int = workItems.size): String {
        val headers = listOf("WorkItem", "Duration", "+Lines↓", "-Lines↓", "Files", "Authors", "Commits", "F", "B", "R", "T", "D", "E")
        val rows = workItems.map { workItem ->
            listOf(
                workItem.workKey.toString(),
                formatDuration(workItem.duration),
                workItem.linesAdded.toString(),
                workItem.linesRemoved.toString(),
                workItem.filesChanged.size.toString(),
                formatAuthors(workItem),
                workItem.commits.toString(),
                workItem.absoluteChurnByType[CommitType.FEATURE]?.toString() ?: "-",
                workItem.absoluteChurnByType[CommitType.FIX]?.toString() ?: "-",
                workItem.absoluteChurnByType[CommitType.REFACTOR]?.toString() ?: "-",
                workItem.absoluteChurnByType[CommitType.TEST]?.toString() ?: "-",
                workItem.absoluteChurnByType[CommitType.DOCS]?.toString() ?: "-",
                workItem.absoluteChurnByType[CommitType.ENVIRONMENT]?.toString() ?: "-"
            )
        }
        val table = formatTable(headers, rows)
        val header = "Work Items (${workItems.size} out of $totalCount)"
        return "$header\n$table"
    }

    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        return when {
            days >= 365 -> "${days / 365}y"
            days >= 30 -> "${days / 30}mo"
            days >= 7 -> "${days / 7}w"
            else -> "${days}d"
        }
    }

    private fun formatAuthors(workItem: WorkItem): String {
        return workItem.contributions
            .take(3)
            .joinToString(",") { it.author.name.split(" ").first() }
            .let { if (workItem.contributions.size > 3) "$it..." else it }
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
