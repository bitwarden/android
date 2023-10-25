package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.util.orNullIfBlank
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An overall container for various [BaseUrlInterceptor] implementations for different API groups.
 */
@Singleton
class BaseUrlInterceptors @Inject constructor() {
    var environment: Environment = Environment.Us
        set(value) {
            field = value
            updateBaseUrls(environment = value)
        }

    /**
     * An interceptor for "/api" calls.
     */
    val apiInterceptor: BaseUrlInterceptor = BaseUrlInterceptor()

    /**
     * An interceptor for "/identity" calls.
     */
    val identityInterceptor: BaseUrlInterceptor = BaseUrlInterceptor()

    /**
     * An interceptor for "/events" calls.
     */
    val eventsInterceptor: BaseUrlInterceptor = BaseUrlInterceptor()

    init {
        // Ensure all interceptors begin with a default value
        environment = Environment.Us
    }

    private fun updateBaseUrls(environment: Environment) {
        val environmentUrlData = environment.environmentUrlData
        val baseUrl = environmentUrlData.base.trim()

        // Determine the required base URLs
        val apiUrl: String
        val identityUrl: String
        val eventsUrl: String
        if (baseUrl.isNotEmpty()) {
            apiUrl = "$baseUrl/api"
            identityUrl = "$baseUrl/identity"
            eventsUrl = "$baseUrl/events"
        } else {
            apiUrl =
                environmentUrlData.api.orNullIfBlank() ?: "https://api.bitwarden.com"
            identityUrl =
                environmentUrlData.identity.orNullIfBlank() ?: "https://identity.bitwarden.com"
            eventsUrl =
                environmentUrlData.events.orNullIfBlank() ?: "https://events.bitwarden.com"
        }

        // Update the base URLs
        apiInterceptor.baseUrl = apiUrl
        identityInterceptor.baseUrl = identityUrl
        eventsInterceptor.baseUrl = eventsUrl
    }
}
