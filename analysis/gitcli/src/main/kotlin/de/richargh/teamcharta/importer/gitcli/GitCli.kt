package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.gitmining.app.GitRepositoryMiner
import de.richargh.teamcharta.importer.gitmining.app.api.GitMiningResult
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import kotlin.time.Clock
import kotlin.time.Instant

enum class OutputFormat {
    table,
    jsonl
}

@Command(
    name = "gitcli",
    mixinStandardHelpOptions = true,
    version = ["gitcli 1.0"],
    description = ["Parses a git repository and outputs a summary table of branches and commits."]
)
class GitCli : Callable<Int> {

    @Parameters(index = "0", description = ["Repository path"])
    lateinit var path: String

    @Option(names = ["--since"], description = ["Since when to include commits (default: 6 months ago)"])
    var since: String = "6 months ago"

    @Option(names = ["--format"], required = true, description = ["Output format"])
    lateinit var format: OutputFormat

    @Option(names = ["--output", "-o"], description = ["Output file path (default: stdout)"])
    var output: String? = null

    private val miner = GitRepositoryMiner()

    override fun call(): Int {
        val now = Clock.System.now()
        val result = miner.parse(File(path), since, now)

        val content = when (format) {
            OutputFormat.table -> formatTable(result, now)
            OutputFormat.jsonl -> formatJsonl(result)
        }

        if (output != null) {
            File(output!!).writeText(content)
            println("Written to $output")
        } else {
            println(content)
        }

        return 0
    }

    private fun formatTable(result: GitMiningResult, now: Instant): String {
        val allAuthorStats = result.authorStatistics.all().toList()
        val displayedAuthorStats = allAuthorStats
            .sortedByDescending { it.commitCount }
            .take(20)

        val branches = result.branches.all().toList()

        val allWorkItems = result.workItems.all().toList()
            .sortedByDescending { it.linesAdded + it.linesRemoved }
        val displayedWorkItems = allWorkItems.take(20)

        val allCommits = result.commits.all().toList()
        val displayedCommits = allCommits.take(20)

        return buildString {
            appendLine(TableFormatter.formatAuthorStatistics(displayedAuthorStats, totalCount = allAuthorStats.size))
            appendLine()
            appendLine(TableFormatter.formatBranches(branches, now))
            appendLine()
            appendLine(TableFormatter.formatWorkItems(displayedWorkItems, totalCount = allWorkItems.size))
            appendLine()
            append(TableFormatter.formatCommits(displayedCommits, totalCount = allCommits.size))
        }
    }

    private fun formatJsonl(result: GitMiningResult): String {
        val allCommits = result.commits.all().toList()
        return JsonlFormatter.format(allCommits)
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(GitCli()).execute(*args))
