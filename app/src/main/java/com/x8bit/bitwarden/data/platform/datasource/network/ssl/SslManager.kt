package com.x8bit.bitwarden.data.platform.datasource.network.ssl

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

/**
 * Interface for managing SSL connections.
 */
interface SslManager {

    /**
     * The SSL context to use for SSL connections.
     */
    val sslContext: SSLContext?

    /**
     * The trust managers to use for SSL connections.
     */
    val trustManagers: Array<TrustManager>
}
