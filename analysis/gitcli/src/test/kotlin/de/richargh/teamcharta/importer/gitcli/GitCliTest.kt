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
}
