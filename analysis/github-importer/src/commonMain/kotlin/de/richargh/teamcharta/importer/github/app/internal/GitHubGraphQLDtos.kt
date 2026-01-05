package de.richargh.teamcharta.importer.github.app.internal

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * GraphQL PageInfo for cursor-based pagination.
 * See: https://docs.github.com/en/graphql/reference/objects#pageinfo
 */
@Serializable
data class GraphQLPageInfo(
    val hasNextPage: Boolean = false,
    val endCursor: String? = null
)

/**
 * GraphQL Author (User) for issue author and timeline actors.
 * See: https://docs.github.com/en/graphql/reference/objects#user
 */
@Serializable
data class GraphQLAuthor(
    val login: String
)

/**
 * GraphQL Label for issue labels.
 * See: https://docs.github.com/en/graphql/reference/objects#label
 */
@Serializable
data class GraphQLLabel(
    val name: String,
    val color: String? = null
)

/**
 * GraphQL Milestone for issue milestones.
 * See: https://docs.github.com/en/graphql/reference/objects#milestone
 */
@Serializable
data class GraphQLMilestone(
    val title: String,
    val state: String  // "OPEN" or "CLOSED" in GraphQL
)

/**
 * GraphQL Repository Owner for repository references.
 */
@Serializable
data class GraphQLRepositoryOwner(
    val login: String
)

/**
 * GraphQL Repository Reference for parent issue cross-repo detection.
 */
@Serializable
data class GraphQLRepositoryRef(
    val owner: GraphQLRepositoryOwner,
    val name: String
)

/**
 * GraphQL Issue Reference for parent/subIssues hierarchy.
 * Contains minimal issue data for relationship tracking.
 * Parent includes repository info for cross-repo detection.
 */
@Serializable
data class GraphQLIssueReference(
    val number: Int,
    val repository: GraphQLRepositoryRef? = null
)

/**
 * GraphQL Connection wrapper for paginated lists.
 * Used for labels, assignees, subIssues, and timelineItems.
 */
@Serializable
data class GraphQLConnection<T>(
    val nodes: List<T> = emptyList(),
    val pageInfo: GraphQLPageInfo? = null
)

/**
 * GraphQL Issue DTO - represents a GitHub issue from the GraphQL API.
 * See: https://docs.github.com/en/graphql/reference/objects#issue
 */
@Serializable
data class GraphQLIssue(
    val number: Int,
    val title: String,
    val state: String,  // "OPEN" or "CLOSED" in GraphQL
    val body: String? = null,
    val createdAt: String,
    val closedAt: String? = null,
    val author: GraphQLAuthor? = null,
    val labels: GraphQLConnection<GraphQLLabel>? = null,
    val assignees: GraphQLConnection<GraphQLAuthor>? = null,
    val milestone: GraphQLMilestone? = null,
    val parent: GraphQLIssueReference? = null,
    val subIssues: GraphQLConnection<GraphQLIssueReference>? = null,
    val timelineItems: GraphQLConnection<GraphQLTimelineItem>? = null
)

// GraphQL Response Wrapper DTOs

/**
 * GraphQL Error response structure.
 */
@Serializable
data class GraphQLError(
    val message: String,
    val type: String? = null
)

/**
 * GraphQL Issue Connection - paginated list of issues.
 */
@Serializable
data class GraphQLIssueConnection(
    val nodes: List<GraphQLIssue> = emptyList(),
    val pageInfo: GraphQLPageInfo? = null
)

/**
 * GraphQL Repository wrapper for issues connection.
 */
@Serializable
data class GraphQLRepository(
    val issues: GraphQLIssueConnection? = null
)

/**
 * GraphQL Data wrapper containing repository.
 */
@Serializable
data class GraphQLData(
    val repository: GraphQLRepository? = null
)

/**
 * GraphQL Response - top-level response wrapper.
 * See: https://graphql.org/learn/serving-over-http/#response
 */
@Serializable
data class GraphQLResponse(
    val data: GraphQLData? = null,
    val errors: List<GraphQLError>? = null
)

/**
 * Custom serializer for polymorphic timeline events using __typename as discriminator.
 * Handles both serialization and deserialization.
 * Unknown event types are returned as Other with the type name.
 */
object GraphQLTimelineItemSerializer : KSerializer<GraphQLTimelineItem> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): GraphQLTimelineItem {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        val typeName = element.jsonObject["__typename"]?.jsonPrimitive?.content

        return when (typeName) {
            "LabeledEvent" -> decoder.json.decodeFromJsonElement(GraphQLTimelineItem.LabeledEvent.serializer(), element)
            "UnlabeledEvent" -> decoder.json.decodeFromJsonElement(GraphQLTimelineItem.UnlabeledEvent.serializer(), element)
            "AssignedEvent" -> decoder.json.decodeFromJsonElement(GraphQLTimelineItem.AssignedEvent.serializer(), element)
            "UnassignedEvent" -> decoder.json.decodeFromJsonElement(GraphQLTimelineItem.UnassignedEvent.serializer(), element)
            "ClosedEvent" -> decoder.json.decodeFromJsonElement(GraphQLTimelineItem.ClosedEvent.serializer(), element)
            "ReopenedEvent" -> decoder.json.decodeFromJsonElement(GraphQLTimelineItem.ReopenedEvent.serializer(), element)
            "MilestonedEvent" -> decoder.json.decodeFromJsonElement(GraphQLTimelineItem.MilestonedEvent.serializer(), element)
            "DemilestonedEvent" -> decoder.json.decodeFromJsonElement(GraphQLTimelineItem.DemilestonedEvent.serializer(), element)
            else -> GraphQLTimelineItem.Other(typeName ?: "Unknown")
        }
    }

    override fun serialize(encoder: Encoder, value: GraphQLTimelineItem) {
        require(encoder is JsonEncoder)
        val jsonElement = when (value) {
            is GraphQLTimelineItem.LabeledEvent -> addTypename("LabeledEvent", encoder.json.encodeToJsonElement(GraphQLTimelineItem.LabeledEvent.serializer(), value))
            is GraphQLTimelineItem.UnlabeledEvent -> addTypename("UnlabeledEvent", encoder.json.encodeToJsonElement(GraphQLTimelineItem.UnlabeledEvent.serializer(), value))
            is GraphQLTimelineItem.AssignedEvent -> addTypename("AssignedEvent", encoder.json.encodeToJsonElement(GraphQLTimelineItem.AssignedEvent.serializer(), value))
            is GraphQLTimelineItem.UnassignedEvent -> addTypename("UnassignedEvent", encoder.json.encodeToJsonElement(GraphQLTimelineItem.UnassignedEvent.serializer(), value))
            is GraphQLTimelineItem.ClosedEvent -> addTypename("ClosedEvent", encoder.json.encodeToJsonElement(GraphQLTimelineItem.ClosedEvent.serializer(), value))
            is GraphQLTimelineItem.ReopenedEvent -> addTypename("ReopenedEvent", encoder.json.encodeToJsonElement(GraphQLTimelineItem.ReopenedEvent.serializer(), value))
            is GraphQLTimelineItem.MilestonedEvent -> addTypename("MilestonedEvent", encoder.json.encodeToJsonElement(GraphQLTimelineItem.MilestonedEvent.serializer(), value))
            is GraphQLTimelineItem.DemilestonedEvent -> addTypename("DemilestonedEvent", encoder.json.encodeToJsonElement(GraphQLTimelineItem.DemilestonedEvent.serializer(), value))
            is GraphQLTimelineItem.Other -> addTypename(value.typeName, encoder.json.encodeToJsonElement(GraphQLTimelineItem.Other.serializer(), value))
        }
        encoder.encodeJsonElement(jsonElement)
    }

    private fun addTypename(typeName: String, element: JsonElement): JsonObject {
        return buildJsonObject {
            put("__typename", typeName)
            element.jsonObject.forEach { (key, value) ->
                if (key != "__typename") { // Avoid duplicating
                    put(key, value)
                }
            }
        }
    }
}

/**
 * Polymorphic container for timeline events using __typename as discriminator.
 * See: https://docs.github.com/en/graphql/reference/unions#issuetimelineitems
 *
 * The __typename field is used by GitHub's GraphQL API as a discriminator.
 */
@Serializable(with = GraphQLTimelineItemSerializer::class)
sealed class GraphQLTimelineItem {

    @Serializable
    data class LabeledEvent(
        val createdAt: String,
        val actor: GraphQLAuthor? = null,
        val label: GraphQLLabel
    ) : GraphQLTimelineItem()

    @Serializable
    data class UnlabeledEvent(
        val createdAt: String,
        val actor: GraphQLAuthor? = null,
        val label: GraphQLLabel
    ) : GraphQLTimelineItem()

    @Serializable
    data class AssignedEvent(
        val createdAt: String,
        val actor: GraphQLAuthor? = null,
        val assignee: GraphQLAuthor? = null
    ) : GraphQLTimelineItem()

    @Serializable
    data class UnassignedEvent(
        val createdAt: String,
        val actor: GraphQLAuthor? = null,
        val assignee: GraphQLAuthor? = null
    ) : GraphQLTimelineItem()

    @Serializable
    data class ClosedEvent(
        val createdAt: String,
        val actor: GraphQLAuthor? = null
    ) : GraphQLTimelineItem()

    @Serializable
    data class ReopenedEvent(
        val createdAt: String,
        val actor: GraphQLAuthor? = null
    ) : GraphQLTimelineItem()

    @Serializable
    data class MilestonedEvent(
        val createdAt: String,
        val actor: GraphQLAuthor? = null,
        val milestoneTitle: String
    ) : GraphQLTimelineItem()

    @Serializable
    data class DemilestonedEvent(
        val createdAt: String,
        val actor: GraphQLAuthor? = null,
        val milestoneTitle: String
    ) : GraphQLTimelineItem()

    /**
     * Fallback for unknown timeline event types.
     * GitHub has many event types we don't need to handle specially.
     */
    @Serializable
    data class Other(
        val typeName: String
    ) : GraphQLTimelineItem()
}
