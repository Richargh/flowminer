package de.richargh.flowminer.importer.jira.app.api

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JiraCredentialsTest {

    @Test
    fun `should store Basic auth credentials`() {
        val auth = JiraAuth.Basic("user", "token")
        val credentials = JiraCredentials(auth, "https://example.atlassian.net")

        auth.username shouldBe "user"
        auth.token shouldBe "token"
        credentials.baseUrl shouldBe "https://example.atlassian.net"
    }

    @Test
    fun `should store PAT credentials`() {
        val auth = JiraAuth.Pat("my-pat")
        val credentials = JiraCredentials(auth, "https://jira.example.com")

        auth.token shouldBe "my-pat"
        credentials.baseUrl shouldBe "https://jira.example.com"
    }

    @Test
    fun `should default to auto api version detection`() {
        val credentials = JiraCredentials(JiraAuth.Pat("token"), "https://jira.example.com")

        credentials.apiVersion shouldBe "auto"
    }

    @Test
    fun `should allow overriding api version`() {
        val credentials = JiraCredentials(JiraAuth.Pat("token"), "https://jira.example.com", apiVersion = "2")

        credentials.apiVersion shouldBe "2"
    }
}
