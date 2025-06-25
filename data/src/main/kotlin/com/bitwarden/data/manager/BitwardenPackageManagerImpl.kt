package com.bitwarden.data.manager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Primary implementation of [BitwardenPackageManager].
 */
class BitwardenPackageManagerImpl(
    context: Context,
) : BitwardenPackageManager {

    private val nativePackageManager = context.packageManager

    override fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                nativePackageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0L),
                )
            } else {
                nativePackageManager.getApplicationInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getAppLabelForPackageOrNull(packageName: String): String? {
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
