package de.richargh.teamcharta.importer.githubfixtures

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Instant

class StateTransitionBuilderTest {

    @Test
    fun `aStateTransition should create StateTransition with specified values`() {
        // given
        val instant = Instant.parse("2024-01-15T10:00:00Z")

        // when
        val transition = aStateTransition {
            field("state")
            from("open")
            to("closed")
            at(instant)
            actor("octocat")
        }

        // then
        transition.field shouldBe "state"
        transition.from shouldBe "open"
        transition.to shouldBe "closed"
        transition.at shouldBe instant
        transition.actor shouldBe "octocat"
    }

    @Test
    fun `aStateTransition should use default values when not specified`() {
        // when
        val transition = aStateTransition()

        // then
        transition.field shouldBe "state"
        transition.from shouldBe null
        transition.to shouldBe null
        transition.actor shouldBe null
    }
}
