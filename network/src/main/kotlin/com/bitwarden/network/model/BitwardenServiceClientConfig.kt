package com.bitwarden.network.model

import com.bitwarden.network.BitwardenServiceClient
import com.bitwarden.network.interceptor.AuthTokenProvider
import com.bitwarden.network.interceptor.BaseUrlsProvider
import com.bitwarden.network.provider.AppIdProvider
import com.bitwarden.network.ssl.CertificateProvider
import java.time.Clock

/**
 * Models configuration for [BitwardenServiceClient].
 */
data class BitwardenServiceClientConfig(
    val clientData: ClientData,
    val appIdProvider: AppIdProvider,
    val baseUrlsProvider: BaseUrlsProvider,
    val authTokenProvider: AuthTokenProvider,
    val certificateProvider: CertificateProvider,
    val clock: Clock = Clock.systemDefaultZone(),
    val enableHttpBodyLogging: Boolean = false,
) {
    /**
     * Models data about the client application.
     */
    data class ClientData(
        val userAgent: String,
        val clientName: String,
        val clientVersion: String,
    )
}
