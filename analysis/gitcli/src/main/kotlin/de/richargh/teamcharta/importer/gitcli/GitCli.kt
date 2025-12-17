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
        val result = miner.parse(File(path), since, ZonedDateTime.now())

        println("## Branches (max 20)")
        println()
        val branches = result.branches.all().take(20).toList()
        println(TableFormatter.formatBranches(branches))

        println()
        println("## Commits (max 20)")
        println()
        val commits = result.commits.all().take(20).toList()
        println(TableFormatter.formatCommits(commits))

        return 0
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(GitCli()).execute(*args))
