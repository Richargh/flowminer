package de.richargh.teamcharta.importer.gitcli

import de.richargh.teamcharta.importer.gitmining.app.GitRepositoryMiner
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.time.ZonedDateTime
import java.util.concurrent.Callable
import kotlin.system.exitProcess

enum class OutputFormat {
    table
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

    private val miner = GitRepositoryMiner()

    override fun call(): Int {
        val now = ZonedDateTime.now()
        val result = miner.parse(File(path), since, now)

        val branches = result.branches.all().toList()
        println(TableFormatter.formatBranches(branches, now))

        println()
        val allCommits = result.commits.all().toList()
        val displayedCommits = allCommits.take(20)
        println(TableFormatter.formatCommits(displayedCommits, totalCount = allCommits.size))

        return 0
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(GitCli()).execute(*args))
