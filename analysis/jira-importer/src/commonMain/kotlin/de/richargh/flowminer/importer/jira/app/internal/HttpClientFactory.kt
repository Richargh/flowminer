package de.richargh.flowminer.importer.jira.app.internal

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
