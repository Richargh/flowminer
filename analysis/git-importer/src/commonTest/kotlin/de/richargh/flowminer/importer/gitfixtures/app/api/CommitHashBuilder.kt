package de.richargh.flowminer.importer.gitfixtures.app.api

import de.richargh.flowminer.importer.git.app.api.CommitHash

fun String.hash() = CommitHash(this)
