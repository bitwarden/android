package com.bitwarden.authenticator.data.platform.provider

import com.bitwarden.core.annotation.OmitFromCoverage
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseApiUrl
import com.bitwarden.data.repository.util.baseEventsUrl
import com.bitwarden.data.repository.util.baseIdentityUrl
import com.bitwarden.network.interceptor.BaseUrlsProvider

/**
 * Default implementation of [BaseUrlsProvider].
 */
@OmitFromCoverage
object BaseUrlsProviderImpl : BaseUrlsProvider {
    override fun getBaseApiUrl(): String =
        Environment.Us.environmentUrlData.baseApiUrl

    override fun getBaseIdentityUrl(): String =
        Environment.Us.environmentUrlData.baseIdentityUrl

    override fun getBaseEventsUrl(): String =
        Environment.Us.environmentUrlData.baseEventsUrl
}
