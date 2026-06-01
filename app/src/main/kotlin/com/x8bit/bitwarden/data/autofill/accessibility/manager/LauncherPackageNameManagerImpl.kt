package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Intent
import android.content.pm.PackageManager
import java.time.Clock

/**
 * How frequently the cached launcher list should be refreshed.
 */
private const val REFRESH_CACHE_MS: Long = 1L * 60L * 60L * 1000L

/**
 * The default implementation of the [LauncherPackageNameManager].
 */
class LauncherPackageNameManagerImpl(
    private val clockProvider: () -> Clock,
    private val packageManager: PackageManager,
) : LauncherPackageNameManager {
    private var lastLauncherFetchMs: Long = 0L
    private var cachedLauncherPackages: List<String>? = null

    override val launcherPackages: List<String>
        get() {
            if (cachedLauncherPackages == null ||
                clockProvider().millis() - lastLauncherFetchMs > REFRESH_CACHE_MS
            ) {
                updateCachedLauncherPackages()
            }
            return cachedLauncherPackages.orEmpty()
        }

    private fun updateCachedLauncherPackages() {
        cachedLauncherPackages = packageManager
            .queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                0,
            )
            .map { it.activityInfo.packageName }
        lastLauncherFetchMs = clockProvider().millis()
    }
}
