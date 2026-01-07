package de.richargh.flowminer.importer.github.app.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun createHttpClient(): HttpClient = HttpClient(Js)
