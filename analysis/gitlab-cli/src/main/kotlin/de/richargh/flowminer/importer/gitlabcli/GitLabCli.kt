package de.richargh.flowminer.importer.gitlabcli

import de.richargh.flowminer.importer.gitlab.app.api.GitLabCredentials
import de.richargh.flowminer.importer.gitlab.app.GitLabCiImporter
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "gitlab-pipelines",
    mixinStandardHelpOptions = true,
    description = ["Fetches the 10 most recent GitLab CI pipelines for a project and writes job data as JSONL."],
    footer = [
        "",
        "Creating a GitLab Token:",
        "  1. Go to https://gitlab.com/-/user_settings/personal_access_tokens",
        "     (self-hosted: https://<your-host>/-/user_settings/personal_access_tokens)",
        "  2. Click 'Add new token'",
        "  3. Give it a name and an expiry date",
        "  4. Select the scope: read_api",
        "  5. Click 'Create personal access token' and copy it",
        "",
        "Storing the Token (set GITLAB_TOKEN env var):",
        "  fish:  set -Ux GITLAB_TOKEN glpat-xxx",
        "  zsh:   echo 'export GITLAB_TOKEN=glpat-xxx' >> ~/.zshrc",
        "  bash:  echo 'export GITLAB_TOKEN=glpat-xxx' >> ~/.bashrc",
        "",
        "Finding the Project ID:",
        "  Option A — numeric ID: open the project in GitLab, the ID is shown",
        "             below the project name on the overview page (e.g. 42)",
        "  Option B — path:       use the namespace/project slug instead",
        "             (e.g. mygroup/myrepo or myuser/myrepo)",
        "",
        "Examples:",
        "  gitlab-pipelines --url https://gitlab.com --token glpat-xxx --project 42 --output pipelines.jsonl",
        $$"  gitlab-pipelines -u https://gitlab.com -t $GITLAB_TOKEN -p mygroup/myrepo -o pipelines.jsonl"
    ]
)
class GitLabCli : Callable<Int> {

    @Option(names = ["--url", "-u"], required = true, description = ["GitLab host URL"])
    lateinit var host: String

    @Option(names = ["--token", "-t"], required = true, description = ["GitLab personal access token"])
    lateinit var token: String

    @Option(names = ["--project", "-p"], required = true, description = ["Project ID or path"])
    lateinit var projectId: String

    @Option(names = ["--output", "-o"], required = true, description = ["Output JSONL file path"])
    lateinit var outputPath: String

    @Option(names = ["--quiet", "-q"], description = ["Suppress progress output"])
    var quiet: Boolean = false

    override fun call(): Int {
        val credentials = GitLabCredentials(token = token, host = host)
        val importer = GitLabCiImporter(credentials)
        val log: (String) -> Unit = if (quiet) ({}) else ::println

        val jobs = runBlocking { importer.import(projectId, log = log) }
        importer.close()
        val outputFile = File(outputPath)

        writeJobsAsJsonl(jobs, outputFile)
        println("Wrote ${jobs.size} jobs to $outputPath")
        return 0
    }
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(GitLabCli()).execute(*args)
    exitProcess(exitCode)
}
