package com.x8bit.bitwarden.data.credentials.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the privileged application allow list JSON used in conjunction with Credential Manager
 * requests.
 */
@Serializable
data class PrivilegedAppAllowListJson(
    @SerialName("apps")
    val apps: List<PrivilegedAppJson>,
) {
    /**
     * Models the privileged application JSON.
     */
    @Serializable
    data class PrivilegedAppJson(
        @SerialName("type")
        val type: String,

        @SerialName("info")
        val info: InfoJson,
    ) {
        /**
         * Models the privileged application info JSON.
         */
        @Serializable
        data class InfoJson(
            @SerialName("package_name")
            val packageName: String,

            @SerialName("signatures")
            val signatures: List<SignatureJson>,
        ) {
            /**
             * Models the privileged application signature JSON.
             */
            @Serializable
            data class SignatureJson(
                @SerialName("build")
                val build: String,

                @SerialName("cert_fingerprint_sha256")
                val certFingerprintSha256: String,
            )
        }
    }
}
