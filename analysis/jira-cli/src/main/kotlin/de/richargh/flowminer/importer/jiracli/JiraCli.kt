package de.richargh.flowminer.importer.jiracli

import de.richargh.flowminer.importer.jira.app.JiraImporter
import de.richargh.flowminer.importer.jira.app.api.JiraAuth
import de.richargh.flowminer.importer.jira.app.api.JiraConnectionException
import de.richargh.flowminer.importer.jira.app.api.JiraCredentials
import de.richargh.flowminer.importer.jira.app.api.JiraForbiddenException
import de.richargh.flowminer.importer.jira.app.api.JiraNotFoundException
import de.richargh.flowminer.importer.jira.app.api.JiraRateLimitException
import de.richargh.flowminer.importer.jira.app.api.JiraUnauthorizedException
import de.richargh.flowminer.importer.jira.app.api.JiraVersionDetectionException
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "jira-issues",
    mixinStandardHelpOptions = true,
    description = ["Fetches all Jira issues for a project and writes work item data as JSONL."],
    footer = [
        "",
        "Creating a Jira API Token:",
        "  Atlassian Cloud (*.atlassian.net):",
        "    1. Go to https://id.atlassian.com/manage-profile/security/api-tokens",
        "    2. Click 'Create API token', give it a label, click 'Create'",
        "    3. Copy the token — it won't be shown again",
        "  Self-hosted / custom domain (Jira Data Center 8.14+ or Server 8.14+):",
        "    1. Go to https://<your-jira-host>/secure/ViewProfile.jspa",
        "    2. Open the 'Personal Access Tokens' tab",
        "    3. Click 'Create token', give it a name and optional expiry",
        "    4. Copy the token — it won't be shown again",
        "  Older self-hosted Jira (pre-8.14, no PAT support):",
        "    Use your regular Jira password as the token value",
        "",
        "Storing the Token:",
        "  Set JIRA_TOKEN so the token never appears in your shell history:",
        "  fish:  set -Ux JIRA_TOKEN your-token-here",
        "  zsh:   echo 'export JIRA_TOKEN=your-token-here' >> ~/.zshrc && source ~/.zshrc",
        "  bash:  echo 'export JIRA_TOKEN=your-token-here' >> ~/.bashrc && source ~/.bashrc",
        "",
        "  To enter it interactively without it appearing in history at all:",
        "  bash/zsh:  read -rs JIRA_TOKEN && export JIRA_TOKEN",
        $$"  fish:      read -s JIRA_TOKEN && set -x JIRA_TOKEN $JIRA_TOKEN",
        "",
        "Finding the Project Key:",
        "  Open the project in Jira — the key is shown in the URL and project settings",
        "  (e.g. PROJ, MYTEAM, ENG)",
        "",
        "Examples:",
        "  jira-issues --url https://myorg.atlassian.net --username me@example.com --token abc123 --project PROJ --output issues.jsonl",
        $$"  jira-issues -u https://myorg.atlassian.net -n me@example.com -t $JIRA_TOKEN -p PROJ -o issues.jsonl"
    ]
)
class JiraCli : Callable<Int> {

    @Option(names = ["--url", "-u"], required = true, description = ["Jira base URL (e.g. https://myorg.atlassian.net)"])
    lateinit var url: String

    @Option(names = ["--username", "-n"], required = false, description = ["Jira account email / username (omit when using a Personal Access Token)"])
    var username: String? = null

    @Option(names = ["--token", "-t"], required = true, description = ["Jira API token"])
    lateinit var token: String

    @Option(names = ["--project", "-p"], required = true, description = ["Jira project key (e.g. PROJ)"])
    lateinit var projectKey: String

    @Option(names = ["--output", "-o"], required = true, description = ["Output JSONL file path"])
    lateinit var outputPath: String

    @Option(names = ["--api-version"], description = ["Jira REST API version: 2, 3, or auto (default)"])
    var apiVersion: String = "auto"

    @Option(names = ["--quiet", "-q"], description = ["Suppress progress output"])
    var quiet: Boolean = false

    override fun call(): Int {
        val auth = if (username != null) JiraAuth.Basic(username!!, token) else JiraAuth.Pat(token)
        val credentials = JiraCredentials(auth = auth, baseUrl = url, apiVersion = apiVersion)
        val importer = JiraImporter(credentials)
        val log: (String) -> Unit = if (quiet) ({}) else ::println

        val identity = if (username != null) "as $username" else "using PAT"
        val versionLabel = if (apiVersion == "auto") "auto-detecting api version" else "api v$apiVersion"
        log("Fetching issues for project $projectKey from $url $identity ($versionLabel)...")
        val workItems = try {
            runBlocking {
                importer.import(projectKey) { fetched, total ->
                    if (!quiet) print("\r  ${progressBar(fetched, total)}")
                }
            }.also { if (!quiet) println() }
        } catch (e: JiraVersionDetectionException) {
            System.err.println("Error: ${e.message}")
            System.err.println("  If this is a Jira instance, try forcing the version with --api-version 2 or --api-version 3")
            importer.close()
            return 1
        } catch (e: JiraConnectionException) {
            System.err.println("Error: Could not reach Jira — is the URL correct?")
            System.err.println("  URL:   $url")
            System.err.println("  Cause: ${e.cause?.message}")
            val serverInfo = runCatching { runBlocking { importer.serverInfo() } }.getOrNull()
            if (serverInfo != null) {
                System.err.println("  Note: $url responds as Jira (${serverInfo.serverTitle}, ${serverInfo.deploymentType} ${serverInfo.version})")
                System.err.println("  → The base URL is valid but the import request itself failed")
            } else {
                System.err.println("  → $url does not appear to be a Jira instance")
            }
            importer.close()
            return 1
        } catch (e: JiraUnauthorizedException) {
            System.err.println("Error: Authentication failed — check your ${if (username != null) "username and API token" else "Personal Access Token"}")
            if (username != null) System.err.println("  Username: $username")
            val serverInfo = runCatching { runBlocking { importer.serverInfo() } }.getOrNull()
            if (serverInfo != null)
                System.err.println("  Instance: ${serverInfo.serverTitle} (${serverInfo.deploymentType} ${serverInfo.version})")
            else
                System.err.println("  Warning: $url did not respond as a Jira instance")
            importer.close()
            return 1
        } catch (e: JiraForbiddenException) {
            System.err.println("Error: Access denied — your account lacks permission for project $projectKey")
            val serverInfo = runCatching { runBlocking { importer.serverInfo() } }.getOrNull()
            if (serverInfo != null)
                System.err.println("  Instance: ${serverInfo.serverTitle} (${serverInfo.deploymentType} ${serverInfo.version})")
            else
                System.err.println("  Warning: $url did not respond as a Jira instance")
            val user = runCatching { runBlocking { importer.currentUser() } }.getOrNull()
            if (user == null) {
                System.err.println("  Could not verify your ${if (username != null) "token" else "PAT"} — does it have any permissions at all?")
            } else {
                System.err.println("  Authenticated as: ${user.displayName} (${user.emailAddress})")
                val accessible = runCatching { runBlocking { importer.accessibleProjects() } }.getOrNull()
                if (accessible != null && accessible.isNotEmpty()) {
                    System.err.println("  Projects you can access (${accessible.size}):")
                    accessible.forEach { p -> System.err.println("    ${p.key}  ${p.name}") }
                } else {
                    System.err.println("  Your account has no accessible projects at $url")
                }
            }
            importer.close()
            return 1
        } catch (e: JiraNotFoundException) {
            System.err.println("Error: Project '$projectKey' not found at $url")
            val serverInfo = runCatching { runBlocking { importer.serverInfo() } }.getOrNull()
            if (serverInfo != null)
                System.err.println("  Instance: ${serverInfo.serverTitle} (${serverInfo.deploymentType} ${serverInfo.version})")
            else
                System.err.println("  Warning: $url did not respond as a Jira instance")
            System.err.println("  Check that the project key is correct and you have access to it")
            importer.close()
            return 1
        } catch (e: JiraRateLimitException) {
            System.err.println("Error: Jira API rate limit exceeded — wait a moment and try again")
            importer.close()
            return 1
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
            importer.close()
            return 1
        }
        importer.close()

        val outputFile = File(outputPath)
        writeWorkItemsAsJsonl(workItems, outputFile)
        println("Wrote ${workItems.size} issues to $outputPath")
        return 0
    }
}

private fun progressBar(fetched: Int, total: Int, width: Int = 30): String {
    val pct = if (total > 0) fetched.toDouble() / total else 0.0
    val filled = (pct * width).toInt()
    val bar = "█".repeat(filled) + "░".repeat(width - filled)
    return "[$bar] $fetched/$total"
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(JiraCli()).execute(*args)
    exitProcess(exitCode)
}
