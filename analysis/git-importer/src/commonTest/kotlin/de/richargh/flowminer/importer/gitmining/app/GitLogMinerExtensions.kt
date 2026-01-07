import de.richargh.flowminer.importer.git.app.internal.parseCommits
import de.richargh.flowminer.importer.git.app.internal.splitIntoRawCommits
import de.richargh.flowminer.importer.gitmining.app.GitLogMiner
import de.richargh.flowminer.importer.gitmining.app.api.GitMiningResult
import kotlin.time.Instant

fun GitLogMiner.parse(lines: Sequence<String>, currentDate: Instant): GitMiningResult {
    val rawCommits = splitIntoRawCommits(lines)
    val commits = parseCommits(rawCommits)
    return mine(commits, currentDate)
}