package de.richargh.teamcharta.importer.jira

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JiraImporterTest {

    private val server = MockWebServer()

    @BeforeAll
    fun beforeAll() {
        server.start()
    }

    @AfterAll
    fun afterAll() {
        server.shutdown()
    }

    @Test
    fun `extracts issues from mocked jira server`(@TempDir tempDir: Path) {
        val mockResponse = """
        {
          "issues": [
            {
              "key": "PROJ-1",
              "fields": {
                "summary": "Test Issue",
                "issuetype": { "name": "Bug" },
                "status": { "name": "In Progress" }
              },
              "changelog": {
                "histories": [
                  {
                    "created": "2024-06-01T10:00:00.000+0000",
                    "items": [
                      { "field": "status", "toString": "In Progress" }
                    ]
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()
        server.enqueue(MockResponse().setBody(mockResponse).setHeader("Content-Type", "application/json"))
        val baseUrl = server.url("/").toString().removeSuffix("/")
        val outputFile = File(tempDir.toFile(), "output.json")
        val exitCode = JiraImporter().apply {
            username = "user"
            token = "token"
            this.baseUrl = baseUrl
            projectKey = "PROJ"
            output = outputFile.absolutePath
        }.call()
        exitCode shouldBe 0
        outputFile.exists() shouldBe true
        val json = outputFile.readText()
        json shouldContain "Test Issue"
        json shouldContain "PROJ-1"
        json shouldContain "Bug"
        json shouldContain "In Progress"
    }
} 