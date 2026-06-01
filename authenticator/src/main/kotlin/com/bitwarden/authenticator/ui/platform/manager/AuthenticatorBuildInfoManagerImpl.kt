package com.bitwarden.authenticator.ui.platform.manager

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.core.data.manager.BuildInfoManager

/**
 * Implementation of [BuildInfoManager] for Bitwarden Authenticator.
 */
@OmitFromCoverage
class AuthenticatorBuildInfoManagerImpl : BuildInfoManager {
    override val applicationId: String
        get() = BuildConfig.APPLICATION_ID

    override val applicationName: String get() = "Authenticator"

    /**
     * Indicates whether the build is from the F-Droid flavor.
     * This is always false for Authenticator as it does not have an F-Droid compatible flavor.
     */
    override val isFdroid: Boolean
        get() = false

    override val isDevBuild: Boolean
        get() = BuildConfig.BUILD_TYPE == "debug"

    override val versionData: String
        get() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    override val sdkData: String
        get() = BuildConfig.SDK_VERSION

    override val ciBuildInfo: String?
        get() = BuildConfig.CI_INFO.takeUnless { it.isBlank() }

    /**
     * Returns the build flavor name.
     * For Authenticator, this is always an empty string as it does not have different flavors.
     */
    override val buildFlavorName: String
        get() = ""

    override val buildTypeName: String
        get() = when (BuildConfig.BUILD_TYPE) {
            "debug" -> "dev"
            "release" -> "prod"
            else -> BuildConfig.BUILD_TYPE
        }

    override val buildAndFlavor: String get() = BuildConfig.BUILD_TYPE
}
