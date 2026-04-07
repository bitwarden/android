package com.x8bit.bitwarden.ui.platform.manager

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.BuildInfoManager
import com.x8bit.bitwarden.BuildConfig

/**
 * Implementation of [BuildInfoManager] for Bitwarden Password Manager.
 */
@OmitFromCoverage
class BitwardenBuildInfoManagerImpl : BuildInfoManager {
    override val applicationId: String
        get() = BuildConfig.APPLICATION_ID

    override val applicationName: String get() = "Password Manager"

    override val isFdroid: Boolean
        get() = BuildConfig.FLAVOR == "fdroid"

    override val isDevBuild: Boolean
        get() = BuildConfig.BUILD_TYPE == "debug"

    override val versionData: String
        get() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    override val sdkData: String
        get() = BuildConfig.SDK_VERSION

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

    override val buildAndFlavor: String get() = "${BuildConfig.BUILD_TYPE}/${BuildConfig.FLAVOR}"
}
