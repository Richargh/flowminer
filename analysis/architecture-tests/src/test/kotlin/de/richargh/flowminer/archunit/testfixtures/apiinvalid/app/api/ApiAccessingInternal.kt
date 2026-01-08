package de.richargh.flowminer.archunit.testfixtures.apiinvalid.app.api

import de.richargh.flowminer.archunit.testfixtures.apiinvalid.app.internal.InternalHelper

class ApiAccessingInternal {
    private val helper = InternalHelper()

    fun violatingMethod(): String = helper.help()
}
