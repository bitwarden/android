package com.bitwarden.network.model

/**
 * Contains the access token and expiration data for a user.
 */
data class AuthTokenData(
    val userId: String,
    val accessToken: String,
    val expiresAtSec: Long,
)
