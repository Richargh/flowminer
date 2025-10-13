package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api.GitEventSeries
import java.io.File

class GitLogRepositoryParser {
    fun parseRepository(repository: File, defaultBranch: String = "main"): GitEventSeries {
        val gitLogOutput = executeGitLog(repository)
        return GitLogParser().parseLog(gitLogOutput)
    }

    private fun executeGitLog(repository: File): String {
        // Git log format:
        // %H = commit hash
        // %P = parent hashes
        // %an = author name
        // %aI = author date (ISO 8601)
        // %D = ref names (branches, tags)
        // %s = subject (commit message)
        // %n = newline
        val format = "%H|%P|%an|%aI|%D|%s"
        val processBuilder = ProcessBuilder(
            "git", "log",
            "--all",
            "--date-order",
            "--pretty=format:$format",
            "--numstat"
        )
        processBuilder.directory(repository)

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader().readText()
            throw IllegalStateException("Git log command failed: $error")
        }

        return output
    }
}