package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the response from a digital asset link check.
 *
 * Modeled after the [CheckAssetLinks](https://developers.google.com/digital-asset-links/reference/rest/v1/assetlinks/check)
 * response from the Google Digital Asset Links API.
 *
 * @property linked Indicates whether the asset link is linked.
 * @property maxAge From serving time, how much longer the response should be considered valid
 * barring further updates. A duration in seconds with up to nine fractional digits, terminated by
 * 's'. Example: "3.5s".
 * @property debugString Human-readable message containing information intended to help end users
 * understand, reproduce and debug the result.
 */
@Suppress("MaxLineLength")
@Serializable
data class DigitalAssetLinkCheckResponseJson(
    @SerialName("linked")
    val linked: Boolean = false,
    @SerialName("maxAge")
    val maxAge: String?,
    @SerialName("debugString")
    val debugString: String?,
)
