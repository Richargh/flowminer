package de.richargh.teamcharta.importer.jira

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.richargh.teamcharta.importer.jira.app.JiraIssueService
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "jira-extractor",
    mixinStandardHelpOptions = true,
    version = ["1.0"],
    description = ["Extracts issue information from a Jira Cloud project and outputs it as JSON."]
)
class JiraImporter : Callable<Int> {
    @Option(names = ["-u", "--username"], description = ["Jira username/email"], required = true)
    lateinit var username: String

    @Option(names = ["-t", "--token"], description = ["Jira API token"], required = true)
    lateinit var token: String

    @Option(
        names = ["-b", "--base-url"],
        description = ["Jira base URL (e.g., https://yourdomain.atlassian.net)"],
        required = true
    )
    lateinit var baseUrl: String

    @Option(names = ["-p", "--project"], description = ["Jira project key"], required = true)
    lateinit var projectKey: String

    @Option(names = ["-o", "--output"], description = ["Output JSON file path"], required = true)
    lateinit var output: String

    override fun call(): Int {
        val issues = JiraIssueService().fetchIssues(username, token, baseUrl, projectKey)
        val mapper = jacksonObjectMapper()
        File(output).writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(issues))
        println("Extracted " + issues.size + " issues to $output")
        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(JiraImporter()).execute(*args))
} 