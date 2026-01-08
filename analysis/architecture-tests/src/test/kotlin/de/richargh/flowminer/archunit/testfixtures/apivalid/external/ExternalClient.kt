package de.richargh.flowminer.archunit.testfixtures.apivalid.external

import de.richargh.flowminer.archunit.testfixtures.apivalid.app.api.PublicApiModel

class ExternalClient {
    fun useApi(): PublicApiModel = PublicApiModel("external access is valid")
}
