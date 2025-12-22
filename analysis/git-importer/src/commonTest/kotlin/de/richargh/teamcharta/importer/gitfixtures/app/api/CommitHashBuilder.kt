package de.richargh.teamcharta.importer.gitfixtures.app.api

import de.richargh.teamcharta.importer.git.app.api.CommitHash

fun String.hash() = CommitHash(this)
