package com.bitwarden.cxf.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the version of the credential exchange protocol.
 * This is used to ensure compatibility between the client and the server during the
 * credential exchange process.
 *
 * See the FIDO Alliance CXF specification for more details:
 * https://fidoalliance.org/specs/cx/cxf-v1.0-rd-20250313.html#dict-version
 *
 * @property major The major version number. A change in this number indicates a
 * breaking change in the protocol.
 * @property minor The minor version number. A change in this number indicates a
 * non-breaking change, such as an addition or enhancement.
 */
@Serializable
data class CredentialExchangeVersion(
    @SerialName("major")
    val major: Int,
    @SerialName("minor")
    val minor: Int,
)
