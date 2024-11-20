package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseApiUrl
import com.x8bit.bitwarden.data.platform.repository.util.baseEventsUrl
import com.x8bit.bitwarden.data.platform.repository.util.baseIdentityUrl
import com.x8bit.bitwarden.data.platform.repository.util.toEnvironmentUrlsOrDefault
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An overall container for various [BaseUrlInterceptor] implementations for different API groups.
 */
@OmitFromCoverage
@Singleton
class BaseUrlInterceptors @Inject constructor(
    private val environmentDiskSource: EnvironmentDiskSource,
) {
    private val environment: Environment
        get() = environmentDiskSource.preAuthEnvironmentUrlData.toEnvironmentUrlsOrDefault()

    /**
     * An interceptor for "/api" calls.
     */
    val apiInterceptor: BaseUrlInterceptor = BaseUrlInterceptor {
        environment.environmentUrlData.baseApiUrl
    }

    /**
     * An interceptor for "/identity" calls.
     */
    val identityInterceptor: BaseUrlInterceptor = BaseUrlInterceptor {
        environment.environmentUrlData.baseIdentityUrl
    }

    /**
     * An interceptor for "/events" calls.
     */
    val eventsInterceptor: BaseUrlInterceptor = BaseUrlInterceptor {
        environment.environmentUrlData.baseEventsUrl
    }
}
