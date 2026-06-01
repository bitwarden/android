package com.bitwarden.network.model

/**
 * Models a network cookie.
 *
 * @property name The name of the cookie.
 * @property value The value of the cookie.
 */
data class NetworkCookie(
    val name: String,
    val value: String,
)
