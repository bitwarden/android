package com.bitwarden.network.util

/**
 * The key used for the 'authorization' headers.
 */
internal const val HEADER_KEY_AUTHORIZATION: String = "Authorization"

/**
 * The key used for the 'bitwarden-client-name' headers.
 */
internal const val HEADER_KEY_CLIENT_NAME: String = "Bitwarden-Client-Name"

/**
 * The key used for the 'bitwarden-client-version' headers.
 */
internal const val HEADER_KEY_CLIENT_VERSION: String = "Bitwarden-Client-Version"

/**
 * The key used for the 'user-agent' headers.
 */
internal const val HEADER_KEY_USER_AGENT: String = "User-Agent"

/**
 * The key used for the 'device-type' headers.
 */
internal const val HEADER_KEY_DEVICE_TYPE: String = "Device-Type"

/**
 * The bearer prefix used for the 'authorization' headers value.
 */
internal const val HEADER_VALUE_BEARER_PREFIX: String = "Bearer "

/**
 * The value used for the 'device-type' headers.
 */
internal const val HEADER_VALUE_DEVICE_TYPE: String = "0"
