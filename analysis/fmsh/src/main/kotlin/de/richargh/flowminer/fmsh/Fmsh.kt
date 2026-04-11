package de.richargh.flowminer.fmsh

import de.richargh.flowminer.importer.gitcli.GitCli
import de.richargh.flowminer.importer.githubcli.GitHubCli
import de.richargh.flowminer.importer.gitlabcli.GitLabCli
import de.richargh.flowminer.importer.jiracli.JiraCli
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.IVersionProvider
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Spec
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "fmsh",
    mixinStandardHelpOptions = true,
    versionProvider = Fmsh.ManifestVersionProvider::class,
    description = ["Flow-Miner Shell - unified CLI for analysis"],
    subcommands = [
        GitCli::class,
        GitHubCli::class,
        GitLabCli::class,
        JiraCli::class
    ]
)
class Fmsh : Callable<Int> {

    @Spec
    lateinit var spec: CommandSpec

    object ManifestVersionProvider : IVersionProvider {
        override fun getVersion(): Array<String> {
            val pkg = Fmsh::class.java.`package`
            val title = pkg.implementationTitle ?: "fmsh"
            val version = pkg.implementationVersion ?: "dev"
            return arrayOf("$title $version")
        }
    }

    override fun call(): Int {
        spec.commandLine().usage(spec.commandLine().out)
        return 0
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(Fmsh()).execute(*args))
