package com.bitwarden.authenticator.data.platform.provider

import com.bitwarden.annotation.OmitFromCoverage
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
    override fun getBaseApiUrl(): String = Environment.Prod.Us.baseApiUrl
    override fun getBaseIdentityUrl(): String = Environment.Prod.Us.baseIdentityUrl
    override fun getBaseEventsUrl(): String = Environment.Prod.Us.baseEventsUrl
    override fun getBaseFillAssistUrl(): String? = null
}
