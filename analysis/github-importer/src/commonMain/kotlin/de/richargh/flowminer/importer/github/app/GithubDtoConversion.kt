package de.richargh.flowminer.importer.github.app

import de.richargh.flowminer.importer.github.app.api.GitHubWorkItem
import de.richargh.flowminer.importer.github.app.api.StateTransition
import de.richargh.flowminer.model.SerializableGitHubWorkItemDto
import de.richargh.flowminer.model.SerializableStateTransitionDto

fun StateTransition.toDto(): SerializableStateTransitionDto = SerializableStateTransitionDto(
    field = field.name,
    from = from,
    to = to,
    at = at.toString(),
    actor = actor
)

fun GitHubWorkItem.toDto(): SerializableGitHubWorkItemDto = SerializableGitHubWorkItemDto(
    id = id.value,
    title = title,
    state = state.name,
    type = type?.name,
    labels = labels,
    assignees = assignees,
    milestone = milestone,
    parentId = parent?.number?.value,
    childIds = childIds.map { it.value },
    created = created.toString(),
    closed = closed?.toString(),
    transitions = transitions.map { it.toDto() }
)
