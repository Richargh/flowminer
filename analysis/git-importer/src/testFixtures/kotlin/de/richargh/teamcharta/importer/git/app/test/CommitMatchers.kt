package de.richargh.teamcharta.importer.git.app.test

import de.richargh.teamcharta.importer.git.app.api.Commit
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult

fun haveSameBranchAs(expected: Commit) = Matcher<Commit> { actual ->
    val diffs = buildList {
        if (actual.branchId != expected.branchId) {
            add("branch:\n    actual:   ${actual.branchId}\n    expected: ${expected.branchId}")
        }
        if (actual.author != expected.author) {
            add("author:\n    actual:   ${actual.author}\n    expected: ${expected.author}")
        }
        if (actual.message != expected.message) {
            add("message:\n    actual:   \"${actual.message}\"\n    expected: \"${expected.message}\"")
        }
    }

    MatcherResult(
        diffs.isEmpty(),
        { "Commit differs:\n${diffs.joinToString("\n")}" },
        { "Commits should not match but they do" }
    )
}

