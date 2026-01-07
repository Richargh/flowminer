package de.richargh.flowminer.importer.githubfixtures

import de.richargh.flowminer.importer.github.app.api.StateTransition
import de.richargh.flowminer.importer.github.app.api.TransitionField
import kotlin.time.Instant

class StateTransitionBuilder {
    private var field: TransitionField = TransitionField.State
    private var from: String? = null
    private var to: String? = null
    private var at: Instant = Instant.parse("2024-01-15T10:00:00Z")
    private var actor: String? = null

    fun field(field: TransitionField) = apply { this.field = field }
    fun from(from: String?) = apply { this.from = from }
    fun to(to: String?) = apply { this.to = to }
    fun at(at: Instant) = apply { this.at = at }
    fun actor(actor: String?) = apply { this.actor = actor }

    fun build(): StateTransition = StateTransition(
        field = field,
        from = from,
        to = to,
        at = at,
        actor = actor
    )
}

fun aStateTransition(block: StateTransitionBuilder.() -> Unit = {}): StateTransition =
    StateTransitionBuilder().apply(block).build()
