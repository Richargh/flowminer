package de.richargh.flowminer.importer.gitlab.app.internal

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
