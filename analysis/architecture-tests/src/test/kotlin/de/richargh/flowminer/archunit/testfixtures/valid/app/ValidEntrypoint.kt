package de.richargh.flowminer.archunit.testfixtures.valid.app

import de.richargh.flowminer.archunit.testfixtures.valid.app.internal.InternalService

class ValidEntrypoint {
    private val internalService = InternalService()

    fun execute(): String = internalService.doInternalWork()
}
