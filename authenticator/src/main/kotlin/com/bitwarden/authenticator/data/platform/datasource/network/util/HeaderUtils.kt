package com.bitwarden.authenticator.data.platform.datasource.network.util

import android.os.Build
import com.bitwarden.authenticator.BuildConfig

/**
 * The bearer prefix used for the 'authorization' headers value.
 */
const val HEADER_VALUE_BEARER_PREFIX: String = "Bearer "

/**
 * The key used for the 'authorization' headers.
 */
const val HEADER_KEY_AUTHORIZATION: String = "Authorization"

/**
 * The key used for the 'user-agent' headers.
 */
const val HEADER_KEY_USER_AGENT: String = "User-Agent"

/**
 * The value used for the 'user-agent' headers.
 */
@Suppress("MaxLineLength")
val HEADER_VALUE_USER_AGENT: String =
    "Bitwarden_Mobile/${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE}) (Android ${Build.VERSION.RELEASE}; SDK ${Build.VERSION.SDK_INT}; Model ${Build.MODEL})"

/**
 * The key used for the 'bitwarden-client-name' headers.
 */
const val HEADER_KEY_CLIENT_NAME: String = "Bitwarden-Client-Name"

/**
 * The value used for the 'bitwarden-client-name' headers.
 */
const val HEADER_VALUE_CLIENT_NAME: String = "mobile"

/**
 * The key used for the 'bitwarden-client-version' headers.
 */
const val HEADER_KEY_CLIENT_VERSION: String = "Bitwarden-Client-Version"

/**
 * The value used for the 'bitwarden-client-version' headers.
 */
const val HEADER_VALUE_CLIENT_VERSION: String = BuildConfig.VERSION_NAME

/**
 * The key used for the 'device-type' headers.
 */
const val HEADER_KEY_DEVICE_TYPE: String = "Device-Type"

/**
 * The value used for the 'device-type' headers.
 */
const val HEADER_VALUE_DEVICE_TYPE: String = "0"
