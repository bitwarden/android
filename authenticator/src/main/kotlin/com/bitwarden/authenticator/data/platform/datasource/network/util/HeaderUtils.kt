package com.bitwarden.authenticator.data.platform.datasource.network.util

import android.os.Build
import com.bitwarden.authenticator.BuildConfig

/**
 * The value used for the 'user-agent' headers.
 */
@Suppress("MaxLineLength")
val HEADER_VALUE_USER_AGENT: String =
    "Bitwarden_Authenticator_Mobile/${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE}) (Android ${Build.VERSION.RELEASE}; SDK ${Build.VERSION.SDK_INT}; Model ${Build.MODEL})"

/**
 * The value used for the 'bitwarden-client-name' headers.
 */
const val HEADER_VALUE_CLIENT_NAME: String = "mobile"

/**
 * The value used for the 'bitwarden-client-version' headers.
 */
const val HEADER_VALUE_CLIENT_VERSION: String = BuildConfig.VERSION_NAME
