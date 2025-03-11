package com.x8bit.bitwarden.data.autofill.fido2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the FIDO 2 privileged application allow list JSON.
 *
 * ```
 * [
 *     {
 *       "type": "android",
 *       "info": {
 *         "package_name": "net.quetta.browser",
 *         "signatures": [
 *           {
 *             "build": "release",
 *             "cert_fingerprint_sha256": "XX:XX:XX:XX:XX:XX:XX:XX.."
 *           }
 *         ]
 *       }
 *     }
 *]
 * ```
 */
@Serializable
data class PrivilegedAppAllowListJson(
    @SerialName("apps")
    val apps: List<PrivilegedAppJson>,
) {
    /**
     * Models the FIDO 2 privileged application JSON.
     */
    @Serializable
    data class PrivilegedAppJson(
        @SerialName("type")
        val type: String,

        @SerialName("info")
        val info: PrivilegedAppInfoJson,
    ) {
        /**
         * Models the FIDO 2 privileged application info JSON.
         */
        @Serializable
        data class PrivilegedAppInfoJson(
            @SerialName("package_name")
            val packageName: String,

            @SerialName("signatures")
            val signatures: List<PrivilegedAppSignatureJson>,
        ) {
            /**
             * Models the FIDO 2 privileged application signature JSON.
             */
            @Serializable
            data class PrivilegedAppSignatureJson(
                @SerialName("build")
                val build: String,

                @SerialName("cert_fingerprint_sha256")
                val certFingerprintSha256: String,
            )
        }
    }
}
