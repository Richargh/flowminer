package de.richargh.teamcharta.importer.githubcli

import de.richargh.teamcharta.importer.githubfixtures.aGraphQLIssue
import de.richargh.teamcharta.importer.githubfixtures.aGraphQLLabel
import de.richargh.teamcharta.importer.githubfixtures.aGraphQLResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import picocli.CommandLine
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class GitHubCliTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream
    private lateinit var outContent: ByteArrayOutputStream
    private lateinit var errContent: ByteArrayOutputStream
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()

        // Capture stdout and stderr
        originalOut = System.out
        originalErr = System.err
        outContent = ByteArrayOutputStream()
        errContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))
        System.setErr(PrintStream(errContent))
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    @Test
    fun `should fetch issues and output table format`() {
        // given
        val graphqlResponse = aGraphQLResponse {
            issues(
                aGraphQLIssue {
                    number(1)
                    title("Bug report")
                    state("OPEN")
                    createdAt("2024-01-01T10:00:00Z")
                },
            )
        }
        mockServer.enqueue(
            MockResponse()
                .setBody(json.encodeToString(graphqlResponse))
                .addHeader("Content-Type", "application/json")
        )

        val cli = GitHubCli()
        val cmd = CommandLine(cli)

        // when
        val exitCode = cmd.execute(
            "--token", "test-token",
            "--owner", "octocat",
            "--repo", "hello-world",
            "--base-url", mockServer.url("/").toString().removeSuffix("/"),
            "--quiet"
        )

        // then
        exitCode shouldBe 0
        val output = outContent.toString()
        output shouldContain "Bug report"
    }

    @Test
    fun `should fetch issues and output jsonl format`() {
        // given
        val graphqlResponse = aGraphQLResponse {
            issues(
                aGraphQLIssue {
                    number(42)
                    title("Test issue")
                    state("OPEN")
                    createdAt("2024-01-01T10:00:00Z")
                    labels(aGraphQLLabel(name = "bug"))
                }
            )
        }
        mockServer.enqueue(
            MockResponse()
                .setBody(json.encodeToString(graphqlResponse))
                .addHeader("Content-Type", "application/json")
        )

        val cli = GitHubCli()
        val cmd = CommandLine(cli)

        // when
        val exitCode = cmd.execute(
            "--token", "test-token",
            "--owner", "octocat",
            "--repo", "hello-world",
            "--format", "jsonl",
            "--base-url", mockServer.url("/").toString().removeSuffix("/"),
            "--quiet"
        )

        // then
        exitCode shouldBe 0
        val output = outContent.toString()
        output shouldContain "\"id\":42"
        output shouldContain "\"title\":\"Test issue\""
        output shouldContain "\"labels\":[\"bug\"]"
    }

    @Test
    fun `should return exit code 1 on error`() {
        // given - server returns error response
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"message": "Bad credentials"}""")
        )

        val cli = GitHubCli()
        val cmd = CommandLine(cli)

        // when
        val exitCode = cmd.execute(
            "--token", "invalid-token",
            "--owner", "octocat",
            "--repo", "hello-world",
            "--base-url", mockServer.url("/").toString().removeSuffix("/"),
            "--quiet"
        )

        // then
        exitCode shouldBe 1
        val errorOutput = errContent.toString()
        errorOutput shouldContain "Error:"
    }
}
