package de.richargh.teamcharta.model

import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
interface StateTransitionDto {
    val field: String
    val from: String?
    val to: String?
    val at: String  // ISO-8601 format
    val actor: String?
}

@Serializable
data class SerializableStateTransitionDto(
    override val field: String,
    override val from: String?,
    override val to: String?,
    override val at: String,
    override val actor: String?
) : StateTransitionDto
