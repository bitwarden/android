package com.x8bit.bitwarden.data.platform.manager.sdk.log

import com.bitwarden.sdk.LogCallback
import com.bitwarden.sdk.LogLevel

/**
 * Creates and manages the logger for the Bitwarden SDK.
 */
interface SdkLoggerFactory {
    /**
     * The log level to be used with the Bitwarden SDK.
     */
    val logLevel: LogLevel

    /**
     * The [LogCallback] to be used with the Bitwarden SDK.
     */
    fun getLogCallback(): LogCallback
}
