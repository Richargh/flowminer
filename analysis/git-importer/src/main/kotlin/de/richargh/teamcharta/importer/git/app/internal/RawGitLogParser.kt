package de.richargh.teamcharta.importer.git.app.internal

fun splitIntoRawCommits(lines: Sequence<String>): List<RawCommit> {
    val rawCommits = mutableListOf<RawCommit>()
    val headerFields = mutableMapOf<String, String>()
    val body = StringBuilder()
    val files = StringBuilder()
    var section = Section.HEADER

    for (line in lines) {
        when {
            line == "-----COMMIT_START-----" -> {
                // Save previous commit if exists
                if (headerFields.isNotEmpty()) {
                    rawCommits.add(RawCommit(headerFields.toMap(), body.toString().trim(), files.toString().trim()))
                    headerFields.clear()
                    body.clear()
                    files.clear()
                }
                section = Section.HEADER
            }
            line == "-----BODY_START-----" -> {
                section = Section.BODY
            }
            line == "-----FILES_START-----" -> {
                section = Section.FILES
            }
            section == Section.HEADER && line.contains("==>>") -> {
                val (key, value) = line.split("==>>", limit = 2).map { it.trim() }
                headerFields[key] = value
            }
            section == Section.BODY -> {
                if (body.isNotEmpty()) body.append("\n")
                body.append(line)
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
        rawCommits.add(RawCommit(headerFields.toMap(), body.toString().trim(), files.toString().trim()))
    }

    return rawCommits
}

private enum class Section { HEADER, BODY, FILES }

data class RawCommit(
    val headerFields: Map<String, String>,
    val body: String,
    val files: String
)