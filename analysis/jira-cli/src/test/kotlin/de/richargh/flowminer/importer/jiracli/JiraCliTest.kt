package de.richargh.flowminer.importer.jiracli

import de.richargh.flowminer.importer.jira.app.api.IssueKey
import de.richargh.flowminer.importer.jira.app.api.WorkItem
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.time.Instant

class JiraCliTest {

    @Test
    fun `shouldWriteJsonlOutput`(@TempDir tempDir: Path) {
        val outputFile = tempDir.resolve("output.jsonl")
        val now = Instant.parse("2024-01-15T10:00:00Z")
        val items = listOf(
            WorkItem(IssueKey("PROJ-1"), "First issue", "Story", "Done", now, now, emptyList()),
            WorkItem(IssueKey("PROJ-2"), "Second issue", "Bug", "In Progress", now, null, emptyList())
        )

        writeWorkItemsAsJsonl(items, outputFile.toFile())

        val lines = outputFile.readText().trim().split("\n")
        lines.size shouldBe 2
        lines[0] shouldContain """"key":"PROJ-1""""
        lines[1] shouldContain """"key":"PROJ-2""""
    }
}
