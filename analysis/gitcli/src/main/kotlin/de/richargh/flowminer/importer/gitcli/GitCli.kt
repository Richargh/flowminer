package de.richargh.flowminer.importer.gitcli

import de.richargh.flowminer.importer.git.app.api.Commit
import de.richargh.flowminer.importer.gitmining.app.GitLogMiner
import de.richargh.flowminer.importer.gitmining.app.GitRepositoryParser
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

    private val parser = GitRepositoryParser()
    private val miner = GitLogMiner()
    private val now = Clock.System.now()

    override fun call(): Int {
        parser.parse(File(path), since, ::consume)
        return 0
    }

    private fun consume(commits: Sequence<Commit>) {
        val content = when (format) {
            OutputFormat.table -> formatTable(commits, now)
            OutputFormat.jsonl -> formatJsonl(commits)
        }

        val out = output
        if (out != null) {
            File(out).bufferedWriter().use { file ->
                content.forEach { line ->
                    file.write(line)
                    file.newLine()
                }
            }
            println("Written to $out")
        } else {
            content.forEach { line ->
                println(line)
            }
        }
    }

    private fun formatTable(commits: Sequence<Commit>, now: Instant): Sequence<String> {
        val result = miner.mine(commits, now)
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

        return sequence {
            yield(TableFormatter.formatAuthorStatistics(displayedAuthorStats, totalCount = allAuthorStats.size))
            yield("")
            yield(TableFormatter.formatBranches(branches, now))
            yield("")
            yield(TableFormatter.formatWorkItems(displayedWorkItems, totalCount = allWorkItems.size))
            yield("")
            yield(TableFormatter.formatCommits(displayedCommits, totalCount = allCommits.size))
        }
    }

    private fun formatJsonl(commits: Sequence<Commit>): Sequence<String> {
        return JsonlFormatter.format(commits)
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(GitCli()).execute(*args))
