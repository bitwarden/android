package com.bitwarden.authenticator.data.platform.manager.imports.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models the JSON export file from Proton Authenticator.
 *
 * A password protected export has a [salt] and no [entries].
 */
@Serializable
data class ProtonAuthenticatorJsonExport(
    val version: Int,
    val entries: List<Entry>?,
    val salt: String?,
) {
    /**
     * Models a single entry contained within a [ProtonAuthenticatorJsonExport].
     */
    @Serializable
    data class Entry(
        val content: Content,
    ) {
        /**
         * Models the OTP data of an [Entry].
         *
         * @property uri An `otpauth://totp/` URI, or `steam://<secret>` for Steam entries.
         * @property entryType Either `Totp` or `Steam`.
         * @property name The account name, falling back to the issuer for TOTP entries.
         */
        @Serializable
        data class Content(
            val uri: String,
            @SerialName("entry_type")
            val entryType: String,
            val name: String?,
        )
    }
}
