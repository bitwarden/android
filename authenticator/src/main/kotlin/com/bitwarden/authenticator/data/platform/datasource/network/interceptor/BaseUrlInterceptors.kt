package com.bitwarden.authenticator.data.platform.datasource.network.interceptor

import com.bitwarden.authenticator.data.platform.repository.model.Environment
import com.bitwarden.authenticator.data.platform.repository.util.baseApiUrl
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

    init {
        // Ensure all interceptors begin with a default value
        environment = Environment.Us
    }

    private fun updateBaseUrls(environment: Environment) {
        val environmentUrlData = environment.environmentUrlData
        apiInterceptor.baseUrl = environmentUrlData.baseApiUrl
    }
}
