package de.richargh.teamcharta.importer.git

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.richargh.teamcharta.importer.git.app.GitLogParser
import de.richargh.teamcharta.importer.git.app.GitLogRepositoryParser
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "git-importer",
    mixinStandardHelpOptions = true,
    version = ["1.0"],
    description = ["Extracts branch lifecycle events from git log and outputs them as JSON."]
)
class GitLogParserCli : Callable<Int> {

    @Option(
        names = ["-r", "--repository"],
        description = ["Path to git repository"],
        required = false
    )
    var repository: File? = null

    @Option(
        names = ["-i", "--input"],
        description = ["Input git log file (if not using --repository)"],
        required = false
    )
    var inputFile: String? = null

    @Option(
        names = ["-b", "--default-branch"],
        description = ["Default branch name (default: main)"],
        required = false
    )
    var defaultBranch: String = "main"

    @Option(
        names = ["-o", "--output"],
        description = ["Output JSON file path"],
        required = true
    )
    lateinit var output: String

    override fun call(): Int {
        if (repository == null && inputFile == null) {
            System.err.println("Error: Either --repository or --input must be specified")
            return 1
        }

        if (repository != null && inputFile != null) {
            System.err.println("Error: Cannot specify both --repository and --input")
            return 1
        }

        val eventSeries = when {
            repository != null -> {
                println("Parsing git repository: $repository")
                GitLogRepositoryParser().parseRepository(repository!!, defaultBranch)
            }
            inputFile != null -> {
                println("Parsing git log file: $inputFile")
                val logContent = File(inputFile!!).readText()
                GitLogParser().parseLog(logContent, defaultBranch)
            }
            else -> throw IllegalStateException("Unreachable")
        }

        val mapper = createJacksonMapperWithJavaTime()
        File(output).writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventSeries))

        println("Extracted ${eventSeries.events.size} events to $output")
        println("  - BranchCreated: ${eventSeries.events.count { it is de.richargh.teamcharta.importer.git.app.api.BranchCreated }}")
        println("  - CommitMade: ${eventSeries.events.count { it is de.richargh.teamcharta.importer.git.app.api.CommitMade }}")
        println("  - BranchMerged: ${eventSeries.events.count { it is de.richargh.teamcharta.importer.git.app.api.BranchMerged }}")

        return 0
    }
}

fun createJacksonMapperWithJavaTime(): ObjectMapper {
    val mapper = jacksonObjectMapper()
    mapper.registerModule(JavaTimeModule())
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    return mapper
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(GitLogParserCli()).execute(*args))
}
