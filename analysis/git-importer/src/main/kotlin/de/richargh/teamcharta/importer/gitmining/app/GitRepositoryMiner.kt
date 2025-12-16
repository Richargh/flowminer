package de.richargh.teamcharta.importer.gitmining.app

import de.richargh.teamcharta.importer.gitmining.app.api.GitMiningResult
import java.io.File

class GitRepositoryMiner(
    private val gitLogParser: GitLogMiner = GitLogMiner()
) {

    fun parse(repoPath: File, since: String = "6 months ago"): GitMiningResult {
        val process = startGitLog(repoPath, since)
        return process.inputStream.bufferedReader().useLines { lines ->
            gitLogParser.parse(lines)
        }
    }

    private fun startGitLog(repoPath: File, since: String): Process {
        val command = gitLogCommand(since)

        return ProcessBuilder(command)
            .directory(repoPath)
            .redirectErrorStream(true)
            .start()
    }
}

private fun gitLogCommand(since: String): List<String>{
    val format = listOf(
        "-----COMMIT_START-----",
        "%D|%H|%P|%aI|%an|%ae|%s",
        "-----BODY_START-----",
        "%b",
        "-----TRAILERS_START-----",
        "%(trailers)",
        "-----FILES_START-----"
    ).joinToString("%n")

    return listOf(
        "git", "log",
        "--all",
        "--numstat",
        "--topo-order",
        "--since=$since",
        "--pretty=format:$format"
    )
}

//fun main(){
//    println(gitLogCommand("3 months ago").joinToString(" "))
//}
