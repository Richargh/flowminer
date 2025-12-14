package de.richargh.teamcharta.importer.git.app.internal

fun splitIntoRawCommits(lines: Sequence<String>): List<RawCommit> {
    val rawCommits = mutableListOf<RawCommit>()
    val headerFields = mutableMapOf<String, String>()
    val body = StringBuilder()
    val trailers = StringBuilder()
    val files = StringBuilder()
    var section = Section.HEADER

    for (line in lines) {
        when {
            line == "-----COMMIT_START-----" -> {
                // Save previous commit if exists
                if (headerFields.isNotEmpty()) {
                    rawCommits.add(RawCommit(
                        headerFields.toMap(),
                        body.toString().trim(),
                        trailers.toString().trim(),
                        files.toString().trim()
                    ))
                    headerFields.clear()
                    body.clear()
                    trailers.clear()
                    files.clear()
                }
                section = Section.HEADER
            }
            line == "-----BODY_START-----" -> {
                section = Section.BODY
            }
            line == "-----TRAILERS_START-----" -> {
                section = Section.TRAILERS
            }
            line == "-----FILES_START-----" -> {
                section = Section.FILES
            }
            section == Section.HEADER && line.isNotEmpty() -> {
                // Parse single-line header: refs|hash|parents|authorDate|author|authorMail|subject
                val parts = line.split("|", limit = 7)
                if (parts.size >= 7) {
                    headerFields["refs"] = parts[0]
                    headerFields["hash"] = parts[1]
                    headerFields["parents"] = parts[2]
                    headerFields["authorDate"] = parts[3]
                    headerFields["author"] = parts[4]
                    headerFields["authorMail"] = parts[5]
                    headerFields["subject"] = parts[6]
                }
            }
            section == Section.BODY -> {
                if (body.isNotEmpty()) body.append("\n")
                body.append(line)
            }
            section == Section.TRAILERS -> {
                if (trailers.isNotEmpty()) trailers.append("\n")
                trailers.append(line)
            }
            section == Section.FILES -> {
                if (line.isNotEmpty()) {
                    if (files.isNotEmpty()) files.append("\n")
                    files.append(line)
                }
            }
        }
    }

    // Handle last commit
    if (headerFields.isNotEmpty()) {
        rawCommits.add(RawCommit(
            headerFields.toMap(),
            body.toString().trim(),
            trailers.toString().trim(),
            files.toString().trim()
        ))
    }

    return rawCommits
}

private enum class Section { HEADER, BODY, TRAILERS, FILES }

data class RawCommit(
    val headerFields: Map<String, String>,
    val body: String,
    val trailers: String,
    val files: String
)