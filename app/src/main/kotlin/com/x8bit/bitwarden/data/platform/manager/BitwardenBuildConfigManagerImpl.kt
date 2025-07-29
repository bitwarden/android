package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.core.data.manager.BitwardenBuildConfigManager
import com.x8bit.bitwarden.BuildConfig

/**
 * The password manager implementation of [BitwardenBuildConfigManager].
 */
class BitwardenBuildConfigManagerImpl : BitwardenBuildConfigManager {
    override val isFdroid: Boolean
        get() = BuildConfig.FLAVOR == "fdroid"

    override val isDevBuild: Boolean
        get() = BuildConfig.BUILD_TYPE == "debug"

    override val versionData: String
        get() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    override val sdkData: String
        get() = BuildConfig.SDK_VERSION

    override val deviceData: String
        get() = "$deviceBrandModel $osInfo $buildInfo"

    override val ciBuildInfo: String?
        get() = BuildConfig.CI_INFO.takeUnless { it.isBlank() }

    override val buildFlavorName: String
        get() = when (BuildConfig.FLAVOR) {
            "standard" -> ""
            else -> "-${BuildConfig.FLAVOR}"
        }

    override val buildTypeName: String
        get() = when (BuildConfig.BUILD_TYPE) {
            "debug" -> "dev"
            "release" -> "prod"
            else -> BuildConfig.BUILD_TYPE
        }
}
