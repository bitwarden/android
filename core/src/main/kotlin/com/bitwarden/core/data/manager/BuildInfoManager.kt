package com.bitwarden.core.data.manager

/**
 * An manager interface for accessing build information for a Bitwarden client application.
 *
 * This interface provides properties to access various build-related information such as
 * whether the build is an F-Droid flavor, if it's a dev build, and details about the app version,
 * SDK version, device information, and CI build info.
 */
interface BuildInfoManager {

    /**
     * The ID of the running application.
     */
    val applicationId: String

    /**
     * The human readable name of the running application (untranslated).
     */
    val applicationName: String

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
     * A string representing the raw build and flavor types.
     */
    val buildAndFlavor: String
}
