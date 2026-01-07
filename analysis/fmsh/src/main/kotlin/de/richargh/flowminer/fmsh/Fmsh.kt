package de.richargh.flowminer.fmsh

import de.richargh.flowminer.importer.gitcli.GitCli
import de.richargh.flowminer.importer.githubcli.GitHubCli
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.IVersionProvider
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "fmsh",
    mixinStandardHelpOptions = true,
    versionProvider = Fmsh.ManifestVersionProvider::class,
    description = ["Flow-Miner Shell - unified CLI for analysis"],
    subcommands = [
        GitCli::class,
        GitHubCli::class
    ]
)
class Fmsh : Callable<Int> {

    object ManifestVersionProvider : IVersionProvider {
        override fun getVersion(): Array<String> {
            val pkg = Fmsh::class.java.`package`
            val title = pkg.implementationTitle ?: "fmsh"
            val version = pkg.implementationVersion ?: "dev"
            return arrayOf("$title $version")
        }
    }

    override fun call(): Int {
        CommandLine(this).usage(System.out)
        return 0
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(Fmsh()).execute(*args))
