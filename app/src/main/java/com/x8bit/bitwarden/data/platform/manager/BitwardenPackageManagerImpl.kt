package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import android.content.pm.PackageManager

/**
 * Primary implementation of [BitwardenPackageManager].
 */
class BitwardenPackageManagerImpl(
    context: Context,
) : BitwardenPackageManager {

    private val nativePackageManager = context.packageManager

    override fun isPackageInstalled(packageName: String): Boolean {
        return try {
            nativePackageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getAppNameFromPackageNameOrNull(packageName: String): String? {
        return try {
            val appInfo = nativePackageManager.getApplicationInfo(packageName, 0)
            nativePackageManager
                .getApplicationLabel(appInfo)
                .toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
