package de.richargh.flowminer.importer.gitlabfixtures

import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class PipelineJobBuilderTest {

    @Test
    fun `shouldBuildDefaultPipelineJob`() {
        // when
        val job = aPipelineJob()

        // then
        job shouldNotBe null
    }
}
