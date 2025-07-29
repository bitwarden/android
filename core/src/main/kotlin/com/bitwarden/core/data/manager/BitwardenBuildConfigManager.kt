package com.bitwarden.core.data.manager

import android.os.Build

/**
 * An abstraction for managing build configuration data in Bitwarden.
 *
 * This interface provides properties to access various build-related information such as
 * whether the build is an F-Droid flavor, if it's a dev build, and details about the app version,
 * SDK version, device information, and CI build info.
 */
interface BitwardenBuildConfigManager {

    /**
     * The ID of the running application.
     */
    val applicationId: String

    /**
     * The authority for the FileProvider used in the application.
     */
    val fileProviderAuthority: String
        get() = "$applicationId.fileprovider"

    /**
     * A boolean property that indicates whether the current build flavor is "fdroid".
     */
    val isFdroid: Boolean

    /**
     * A boolean property that indicates whether the current build is a dev build.
     */
    val isDevBuild: Boolean

    /**
     * A string that represents a displayable app version.
     */
    val versionData: String

    /**
     * A string that represents a displayable SDK version.
     */
    val sdkData: String

    /**
     * A string that represents device data.
     */
    val deviceData: String
        get() = "$deviceBrandModel $osInfo $buildInfo"

    /**
     * A string representing the CI information if available.
     */
    val ciBuildInfo: String?

    /**
     * A string representing the build flavor or blank if it is the standard configuration.
     */
    val buildFlavorName: String

    /**
     * A string representing the build type.
     */
    val buildTypeName: String

    /**
     * A string representing the device brand and model.
     */
    val deviceBrandModel: String
        get() = "\uD83D\uDCF1 ${Build.BRAND} ${Build.MODEL}"

    /**
     * A string representing the operating system information.
     */
    val osInfo: String
        get() = "\uD83E\uDD16 ${Build.VERSION.RELEASE}@${Build.VERSION.SDK_INT}"

    /**
     * A string representing the build information.
     */
    val buildInfo: String
        get() = "\uD83D\uDCE6 $buildTypeName" +
            buildFlavorName.takeUnless { it.isBlank() }?.let { " $it" }.orEmpty()
}
