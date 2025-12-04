package de.richargh.teamcharta.importer.git

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class GitLogParserCliTest {

    @Test
    fun `should display error when neither repository nor input is specified`(@TempDir tempDir: Path) {
        // Given
        val importer = GitLogParserCli()
        importer.output = tempDir.resolve("output.json").toString()

        // When
        val exitCode = importer.call()

        // Then
        exitCode shouldBe 1
    }

    @Test
    fun `should display error when both repository and input are specified`(@TempDir tempDir: Path) {
        // Given
        val importer = GitLogParserCli()
        importer.repository = File("/some/path")
        importer.inputFile = "some-file.log"
        importer.output = tempDir.resolve("output.json").toString()

        // When
        val exitCode = importer.call()

        // Then
        exitCode shouldBe 1
    }

    @Test
    fun `should successfully parse git log from file`(@TempDir tempDir: Path) {
        // Given
        val gitLogContent = """
            abc123||John Doe|2024-01-15T10:00:00Z|HEAD -> main|Initial commit

            def456|abc123|Jane Smith|2024-01-16T11:00:00Z|main|Add feature
            1	0	src/Feature.kt
        """.trimIndent()

        val inputFile = tempDir.resolve("git.log").toFile()
        inputFile.writeText(gitLogContent)

        val outputFile = tempDir.resolve("output.json").toFile()

        val importer = GitLogParserCli()
        importer.inputFile = inputFile.absolutePath
        importer.output = outputFile.absolutePath

        // When
        val exitCode = importer.call()

        // Then
        exitCode shouldBe 0
        outputFile.exists() shouldBe true

        val jsonContent = outputFile.readText()
        jsonContent shouldContain "\"commitHash\" : \"abc123\""
        jsonContent shouldContain "\"commitHash\" : \"def456\""
        jsonContent shouldContain "\"defaultBranch\" : \"main\""
    }
}
