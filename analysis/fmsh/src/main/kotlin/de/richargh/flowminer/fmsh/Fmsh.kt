package de.richargh.flowminer.fmsh

import de.richargh.flowminer.importer.gitcli.GitCli
import de.richargh.flowminer.importer.githubcli.GitHubCli
import picocli.CommandLine
import picocli.CommandLine.Command
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "fmsh",
    mixinStandardHelpOptions = true,
    version = ["fmsh 1.0"],
    description = ["Flow-Miner Shell - unified CLI for git and GitHub analysis"],
    subcommands = [
        GitCli::class,
        GitHubCli::class
    ]
)
class Fmsh : Callable<Int> {
    override fun call(): Int {
        CommandLine(this).usage(System.out)
        return 0
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(Fmsh()).execute(*args))
