package de.richargh.teamcharta.importer.gitcli

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import picocli.CommandLine

class GitCliTest {

    @Test
    fun `parses path as positional argument`() {
        val cli = GitCli()
        CommandLine(cli).parseArgs("/some/repo", "--format", "table")

        cli.path shouldBe "/some/repo"
    }

    @Test
    fun `parses format as enum`() {
        val cli = GitCli()
        CommandLine(cli).parseArgs("/some/repo", "--format", "table")

        cli.format shouldBe OutputFormat.table
    }

    @Test
    fun `parses since option`() {
        val cli = GitCli()
        CommandLine(cli).parseArgs("/some/repo", "--format", "table", "--since", "3 months ago")

        cli.since shouldBe "3 months ago"
    }

    @Test
    fun `has default since value of 6 months ago`() {
        val cli = GitCli()
        CommandLine(cli).parseArgs("/some/repo", "--format", "table")

        cli.since shouldBe "6 months ago"
    }

    @Test
    fun `parses format jsonl`() {
        val cli = GitCli()
        CommandLine(cli).parseArgs("/some/repo", "--format", "jsonl")

        cli.format shouldBe OutputFormat.jsonl
    }

    @Test
    fun `parses output option with jsonl format`() {
        val cli = GitCli()
        CommandLine(cli).parseArgs("/some/repo", "--format", "jsonl", "--output", "/path/to/output.jsonl")

        cli.output shouldBe "/path/to/output.jsonl"
    }

    @Test
    fun `parses output option with table format`() {
        val cli = GitCli()
        CommandLine(cli).parseArgs("/some/repo", "--format", "table", "--output", "/path/to/output.txt")

        cli.format shouldBe OutputFormat.table
        cli.output shouldBe "/path/to/output.txt"
    }

    @Test
    fun `output is null by default`() {
        val cli = GitCli()
        CommandLine(cli).parseArgs("/some/repo", "--format", "table")

        cli.output shouldBe null
    }

    @Test
    fun `parses short output option`() {
        val cli = GitCli()
        CommandLine(cli).parseArgs("/some/repo", "--format", "jsonl", "-o", "/path/to/output.jsonl")

        cli.output shouldBe "/path/to/output.jsonl"
    }
}
