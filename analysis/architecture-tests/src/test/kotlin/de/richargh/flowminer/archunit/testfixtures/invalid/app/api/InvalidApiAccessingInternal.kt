package de.richargh.flowminer.archunit.testfixtures.invalid.app.api

import de.richargh.flowminer.archunit.testfixtures.invalid.app.internal.InternalService

class InvalidApiAccessingInternal {
    private val internalService = InternalService()

    fun violatingMethod(): String = internalService.doInternalWork()
}
