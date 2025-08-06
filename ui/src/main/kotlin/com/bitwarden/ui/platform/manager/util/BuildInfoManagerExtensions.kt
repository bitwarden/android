@file:OmitFromCoverage

package com.bitwarden.ui.platform.manager.util

import android.os.Build
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.BuildInfoManager

/**
 * A string representing the device brand and model.
 */
val BuildInfoManager.deviceBrandModel: String
    get() = "\uD83D\uDCF1 ${Build.BRAND} ${Build.MODEL}"

/**
 * A string representing the operating system information.
 */
val BuildInfoManager.osInfo: String
    get() = "\uD83E\uDD16 ${Build.VERSION.RELEASE}@${Build.VERSION.SDK_INT}"

/**
 * A string representing the build information.
 */
val BuildInfoManager.buildInfo: String
    get() = "\uD83D\uDCE6 $buildTypeName" +
        buildFlavorName.takeUnless { it.isBlank() }?.let { " $it" }.orEmpty()

/**
 * A string that represents device data.
 */
val BuildInfoManager.deviceData: String
    get() = "$deviceBrandModel $osInfo $buildInfo"

/**
 * The authority for the FileProvider used in the application.
 */
val BuildInfoManager.fileProviderAuthority: String
    get() = "$applicationId.fileprovider"
