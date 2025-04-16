package com.bitwarden.network.interceptor

import com.bitwarden.core.annotation.OmitFromCoverage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An overall container for various [BaseUrlInterceptor] implementations for different API groups.
 */
@OmitFromCoverage
@Singleton
class BaseUrlInterceptors @Inject constructor(
    private val baseUrlsProvider: BaseUrlsProvider,
) {
    /**
     * An interceptor for "/api" calls.
     */
    val apiInterceptor: BaseUrlInterceptor = BaseUrlInterceptor {
        baseUrlsProvider.getBaseApiUrl()
    }

    /**
     * An interceptor for "/identity" calls.
     */
    val identityInterceptor: BaseUrlInterceptor = BaseUrlInterceptor {
        baseUrlsProvider.getBaseIdentityUrl()
    }

    /**
     * An interceptor for "/events" calls.
     */
    val eventsInterceptor: BaseUrlInterceptor = BaseUrlInterceptor {
        baseUrlsProvider.getBaseEventsUrl()
    }
}
