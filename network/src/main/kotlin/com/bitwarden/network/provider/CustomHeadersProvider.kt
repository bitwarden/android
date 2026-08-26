package com.bitwarden.network.provider

/**
 * Provider for user-configured custom headers sent with requests to a self-hosted environment.
 *
 * This supports self-hosted servers sitting behind a reverse proxy that gates access on a
 * header, such as Cloudflare Access service tokens.
 */
interface CustomHeadersProvider {
    /**
     * Retrieves the custom headers to attach to a request to [url].
     *
     * Returns an empty map when no custom headers are configured or when [url] does not belong
     * to the current environment's hosts, so header values, which may contain credentials, are
     * never sent to third parties.
     *
     * @param url The URL of the request the headers will be attached to.
     * @return The custom headers to attach, or an empty map if there are none.
     */
    fun getCustomHeaders(url: String): Map<String, String>
}
