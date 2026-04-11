package de.richargh.flowminer.importer.jirafixtures

import de.richargh.flowminer.importer.jira.app.api.IssueKey
import de.richargh.flowminer.importer.jira.app.api.StateTransition
import de.richargh.flowminer.importer.jira.app.api.WorkItem
import kotlin.time.Instant

fun aWorkItem(block: WorkItemBuilder.() -> Unit = {}): WorkItem =
    WorkItemBuilder().apply(block).build()

class WorkItemBuilder {
    var key: IssueKey = IssueKey("TEST-1")
    var name: String? = "Test Issue"
    var type: String? = "Story"
    var state: String? = "To Do"
    var started: Instant? = null
    var finished: Instant? = null
    var transitions: List<StateTransition> = emptyList()

    fun build(): WorkItem = WorkItem(
        key = key,
        name = name,
        type = type,
        state = state,
        started = started,
        finished = finished,
        transitions = transitions
    )
}
