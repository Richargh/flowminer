package de.richargh.flowminer.importer.jira.app.dto

import de.richargh.flowminer.importer.jira.app.api.IssueKey
import de.richargh.flowminer.importer.jirafixtures.aJiraChangelog
import de.richargh.flowminer.importer.jirafixtures.aJiraFields
import de.richargh.flowminer.importer.jirafixtures.aJiraHistory
import de.richargh.flowminer.importer.jirafixtures.aJiraHistoryItem
import de.richargh.flowminer.importer.jirafixtures.aJiraIssue
import de.richargh.flowminer.importer.jirafixtures.aJiraIssueType
import de.richargh.flowminer.importer.jirafixtures.aJiraStatus
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class WorkItemDtoConversionTest {

    @Test
    fun `should map key name type and state from JiraApiIssueDto`() {
        val issue = aJiraIssue {
            key = "PROJ-1"
            fields = aJiraFields {
                summary = "Bug"
                issuetype = aJiraIssueType { name = "Bug" }
                status = aJiraStatus { name = "In Progress" }
            }
        }

        val workItem = issue.toWorkItem()

        workItem.key shouldBe IssueKey("PROJ-1")
        workItem.name shouldBe "Bug"
        workItem.type shouldBe "Bug"
        workItem.state shouldBe "In Progress"
    }

    @Test
    fun `should detect started from In Progress transition`() {
        val issue = aJiraIssue {
            changelog = aJiraChangelog {
                histories(aJiraHistory {
                    created = "2024-06-01T10:00:00.000+0000"
                    items(aJiraHistoryItem {
                        fromStatus = "To Do"
                        toStatus = "In Progress"
                    })
                })
            }
        }

        val workItem = issue.toWorkItem()

        workItem.started shouldBe Instant.parse("2024-06-01T10:00:00Z")
    }

    @Test
    fun `should detect finished from Done transition`() {
        val issue = aJiraIssue {
            changelog = aJiraChangelog {
                histories(aJiraHistory {
                    created = "2024-06-02T12:00:00.000+0000"
                    items(aJiraHistoryItem {
                        fromStatus = "In Progress"
                        toStatus = "Done"
                    })
                })
            }
        }

        val workItem = issue.toWorkItem()

        workItem.finished shouldBe Instant.parse("2024-06-02T12:00:00Z")
    }

    @Test
    fun `should detect finished from Closed transition`() {
        val issue = aJiraIssue {
            changelog = aJiraChangelog {
                histories(aJiraHistory {
                    created = "2024-06-03T08:00:00.000+0000"
                    items(aJiraHistoryItem {
                        fromStatus = "In Progress"
                        toStatus = "Closed"
                    })
                })
            }
        }

        val workItem = issue.toWorkItem()

        workItem.finished shouldBe Instant.parse("2024-06-03T08:00:00Z")
    }

    @Test
    fun `should build transition list from status changelog items`() {
        val issue = aJiraIssue {
            changelog = aJiraChangelog {
                histories(aJiraHistory {
                    created = "2024-06-01T10:00:00.000+0000"
                    items(aJiraHistoryItem {
                        fromStatus = "To Do"
                        toStatus = "In Progress"
                    })
                })
            }
        }

        val workItem = issue.toWorkItem()

        workItem.transitions.size shouldBe 1
        workItem.transitions[0].from shouldBe "To Do"
        workItem.transitions[0].to shouldBe "In Progress"
        workItem.transitions[0].at shouldBe Instant.parse("2024-06-01T10:00:00Z")
    }

    @Test
    fun `should throw when Jira issue key is null`() {
        val issue = aJiraIssue { key = null }

        assertFailsWith<IllegalArgumentException> {
            issue.toWorkItem()
        }
    }
}

class ParseJiraInstantTest {

    @Test
    fun `should parse UTC offset in compact format`() {
        parseJiraInstant("2024-06-01T10:00:00.000+0000") shouldBe Instant.parse("2024-06-01T10:00:00Z")
    }

    @Test
    fun `should parse non-UTC offset`() {
        parseJiraInstant("2024-06-01T10:00:00.000+0530") shouldBe Instant.parse("2024-06-01T04:30:00Z")
    }

    @Test
    fun `should throw with descriptive message on invalid date`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            parseJiraInstant("not-a-date")
        }
        ex.message?.contains("not-a-date") shouldBe true
    }
}
