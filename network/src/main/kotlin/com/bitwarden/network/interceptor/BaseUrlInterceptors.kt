package com.bitwarden.network.interceptor

import com.bitwarden.annotation.OmitFromCoverage

/**
 * An overall container for various [BaseUrlInterceptor] implementations for different API groups.
 */
@OmitFromCoverage
internal class BaseUrlInterceptors(
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
