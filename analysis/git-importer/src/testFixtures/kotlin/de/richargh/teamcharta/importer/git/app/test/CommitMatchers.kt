package de.richargh.teamcharta.importer.git.app.test

import de.richargh.teamcharta.importer.git.app.api2.Commit
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult

fun haveSameBranchAs(expected: Commit) = Matcher<Commit> { actual ->
    val diffs = buildList {
        if (actual.branch != expected.branch) {
            add("branch:\n    actual:   ${actual.branch}\n    expected: ${expected.branch}")
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

