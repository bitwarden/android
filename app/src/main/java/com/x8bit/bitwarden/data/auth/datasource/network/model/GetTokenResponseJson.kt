package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models json response of the get token request.
 *
 * @param accessToken the access token.
 */
@Serializable
data class GetTokenResponseJson(
    @SerialName("access_token")
    val accessToken: String,
)
