package de.richargh.teamcharta.importer.github.app.internal

import io.ktor.client.HttpClient

/**
 * Platform-specific HttpClient factory.
 * JVM uses the Java engine, JS uses the JS engine.
 */
expect fun createHttpClient(): HttpClient
