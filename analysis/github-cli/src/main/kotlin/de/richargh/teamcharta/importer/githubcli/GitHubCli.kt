package de.richargh.teamcharta.importer.githubcli

import de.richargh.teamcharta.importer.github.app.GitHubImporter
import de.richargh.teamcharta.importer.github.app.api.GitHubCredentials
import de.richargh.teamcharta.importer.github.app.api.GitHubWorkItem
import de.richargh.teamcharta.importer.github.app.api.RepositoryId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

enum class OutputFormat {
    table,
    jsonl
}

@Command(
    name = "github-cli",
    mixinStandardHelpOptions = true,
    version = ["github-cli 1.0"],
    description = ["Fetches GitHub issues and outputs them in various formats."],
    footer = [
        "",
        "Creating a GitHub Token:",
        "  1. Go to https://github.com/settings/tokens",
        "  2. Click 'Generate new token' (recommended: fine-grained, repo-scoped)",
        "  3. Give it a name of your choice",
        "  4. Set Repository access to your target repos (All or selected)",
        "  5. Under Permissions > Repository permissions:",
        "     - Issues: Read-only",
        "     - Metadata: Read-only",
        "  6. Click 'Generate token' and copy it",
        "",
        "Storing the Token (set GITHUB_TOKEN env var):",
        "  fish:  set -Ux GITHUB_TOKEN github_pat_xxx",
        "  zsh:   echo 'export GITHUB_TOKEN=github_pat_xxx' >> ~/.zshrc",
        "  bash:  echo 'export GITHUB_TOKEN=github_pat_xxx' >> ~/.bashrc",
        "",
        "Example:",
        "  github-cli -t github_pat_xxx --owner octocat --repo hello-world",
        $$"  github-cli -t $GITHUB_TOKEN --owner octocat --repo hello-world"
    ]
)
class GitHubCli : Callable<Int> {

    @Option(names = ["--token", "-t"], required = true, description = ["GitHub Personal Access Token or App Token"])
    lateinit var token: String

    @Option(names = ["--owner"], required = true, description = ["Repository owner (user or organization)"])
    lateinit var owner: String

    @Option(names = ["--repo", "-r"], required = true, description = ["Repository name"])
    lateinit var repo: String

    @Option(names = ["--format", "-f"], description = ["Output format (default: table)"])
    var format: OutputFormat = OutputFormat.table

    @Option(names = ["--output", "-o"], description = ["Output file path (default: stdout)"])
    var output: String? = null

    @Option(names = ["--quiet", "-q"], description = ["Suppress progress output"])
    var quiet: Boolean = false

    @Option(names = ["--base-url"], hidden = true, description = ["Base URL for GitHub API (for testing)"])
    var baseUrl: String = "https://api.github.com"

    override fun call(): Int {
        val credentials = GitHubCredentials(token = token, baseUrl = baseUrl)
        val service = GitHubImporter(credentials)
        val repoId = RepositoryId(owner = owner, name = repo)

        return try {
            runBlocking {
                val issues = service.fetchIssuesWithTimelines(repoId, quiet)
                consume(issues)
            }

            0
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
            1
        } finally {
            service.close()
        }
    }

    private suspend fun consume(workItems: Flow<GitHubWorkItem>) {
        val content = when (format) {
            OutputFormat.table -> formatTable(workItems)
            OutputFormat.jsonl -> formatJsonl(workItems)
        }

        writeOutput(content)
    }

    private suspend fun formatTable(workItems: Flow<GitHubWorkItem>): Flow<String> {
        return TableFormatter.format(workItems)
    }

    private fun formatJsonl(workItems: Flow<GitHubWorkItem>): Flow<String> {
        return JsonlFormatter.format(workItems)
    }

    private suspend fun writeOutput(content: Flow<String>) {
        val out = output
        if (out != null) {
            File(out).bufferedWriter().use { file ->
                content.collect { line ->
                    file.write(line)
                    file.newLine()
                }
            }
            println("Written to $out")
        } else {
            content.collect { line ->
                println(line)
            }
        }
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(GitHubCli()).execute(*args))
