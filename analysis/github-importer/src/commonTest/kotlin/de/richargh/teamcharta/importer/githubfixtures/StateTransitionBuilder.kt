package de.richargh.teamcharta.importer.githubfixtures

import de.richargh.teamcharta.importer.github.app.api.StateTransition
import kotlin.time.Instant

class StateTransitionBuilder {
    private var field: String = "state"
    private var from: String? = null
    private var to: String? = null
    private var at: Instant = Instant.parse("2024-01-15T10:00:00Z")
    private var actor: String? = null

    fun field(field: String) = apply { this.field = field }
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
