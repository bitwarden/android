package com.bitwarden.authenticator.data.platform.provider

import com.bitwarden.core.annotation.OmitFromCoverage
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
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
        US_QA_ENV.environmentUrlData.baseApiUrl

    override fun getBaseIdentityUrl(): String =
        US_QA_ENV.environmentUrlData.baseIdentityUrl

    override fun getBaseEventsUrl(): String =
        US_QA_ENV.environmentUrlData.baseEventsUrl
}

private val US_QA_ENV: Environment = Environment.SelfHosted(
    EnvironmentUrlDataJson(
        base = "https://vault.qa.bitwarden.pw",
    ),
)
