package de.richargh.teamcharta.importer.git.app

import de.richargh.teamcharta.importer.git.app.api2.GitMiningResult
import java.io.File

class GitRepositoryParser(
    private val gitLogParser: GitLogParser2 = GitLogParser2()
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
        "hash==>> %H",
        "author==>> %an",
        "authorMail==>> %ae",
        "authorDate==>> %aI",
        "subject==>> %s",
        "parents==>> %P",
        "refs==>> %D",
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
        "--reverse",
        "--since=$since",
        "--pretty=format:$format"
    )
}

//fun main(){
//    println(gitLogCommand("3 months ago").joinToString(" "))
//}
