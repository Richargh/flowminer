package de.richargh.teamcharta.importer.git.app.internal

import de.richargh.teamcharta.importer.git.app.api.BranchName

private val MERGE_PATTERN = """Merge branch(es)? (.+)""".toRegex()
private val BRANCH_NAME_PATTERN = """'([^']+)'""".toRegex()

fun extractAllMergedBranches(message: String): List<BranchName> {
    val match = MERGE_PATTERN.find(message) ?: return emptyList()
    val branchesStr = match.groupValues[2]
    return BRANCH_NAME_PATTERN.findAll(branchesStr)
        .map { BranchName(it.groupValues[1]) }
        .toList()
}
