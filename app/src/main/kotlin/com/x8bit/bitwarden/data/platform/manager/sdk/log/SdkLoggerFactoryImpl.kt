package com.x8bit.bitwarden.data.platform.manager.sdk.log

import android.util.Log
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.sdk.LogCallback
import com.bitwarden.sdk.LogLevel
import timber.log.Timber

/**
 * The default implementation for the [SdkLoggerFactory].
 */
internal class SdkLoggerFactoryImpl(
    private val buildInfoManager: BuildInfoManager,
) : SdkLoggerFactory {
    override val logLevel: LogLevel
        get() = if (buildInfoManager.isDevBuild) LogLevel.DEBUG else LogLevel.INFO

    override fun getLogCallback(): LogCallback = object : LogCallback {
        override fun onLog(level: String, target: String, message: String) {
            Timber
                .tag(tag = "BitwardenSdkClient")
                .log(priority = level.logLevel, message = "$target --$message")
        }
    }
}

private val String.logLevel: Int
    get() = when (this) {
        "TRACE" -> Log.VERBOSE
        "DEBUG" -> Log.DEBUG
        "INFO" -> Log.INFO
        "WARN" -> Log.WARN
        "ERROR" -> Log.ERROR
        else -> Log.ASSERT
    }
