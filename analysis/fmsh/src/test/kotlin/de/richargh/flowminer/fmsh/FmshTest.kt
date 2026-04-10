package de.richargh.flowminer.fmsh

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import picocli.CommandLine
import java.io.PrintWriter
import java.io.StringWriter

class FmshTest {

    @Test
    fun `fmsh --help shows available subcommands`() {
        val (exitCode, output) = runFmsh("--help")

        exitCode shouldBe 0
        output shouldContain "git-commits"
        output shouldContain "github-workitems"
        output shouldContain "gitlab-pipelines"
    }

    @Test
    fun `fmsh with no args shows help`() {
        val (exitCode, output) = runFmsh()

        exitCode shouldBe 0
        output shouldContain "git-commits"
        output shouldContain "github-workitems"
    }

    @Test
    fun `fmsh git-commits --help shows gitcli help`() {
        val (exitCode, output) = runFmsh("git-commits", "--help")

        exitCode shouldBe 0
        output shouldContain "git-commits"
        output shouldContain "Repository path"
    }

    @Test
    fun `fmsh github-workitems --help shows github-cli help`() {
        val (exitCode, output) = runFmsh("github-workitems", "--help")

        exitCode shouldBe 0
        output shouldContain "github-workitems"
        output shouldContain "GitHub Personal Access Token"
    }

    @Test
    fun `fmsh --help shows gitlab-pipelines subcommand`() {
        val (exitCode, output) = runFmsh("--help")

        exitCode shouldBe 0
        output shouldContain "gitlab-pipelines"
    }

    @Test
    fun `fmsh gitlab-pipelines --help shows gitlab-cli help`() {
        val (exitCode, output) = runFmsh("gitlab-pipelines", "--help")

        exitCode shouldBe 0
        output shouldContain "gitlab-pipelines"
        output shouldContain "GitLab personal access token"
        output shouldContain "read_api"
        output shouldContain "GITLAB_TOKEN"
        output shouldContain "Finding the Project ID"
    }

    private fun runFmsh(vararg args: String): Pair<Int, String> {
        val cmd = CommandLine(Fmsh())
        val sw = StringWriter()
        cmd.out = PrintWriter(sw)
        cmd.err = PrintWriter(sw)
        val exitCode = cmd.execute(*args)
        return exitCode to sw.toString()
    }
}
