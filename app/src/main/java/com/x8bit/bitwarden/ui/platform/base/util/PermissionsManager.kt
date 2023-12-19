package com.x8bit.bitwarden.ui.platform.base.util

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Interface for managing permissions.
 */
@Immutable
interface PermissionsManager {

    /**
     * Method for creating and returning a permission launcher.
     */
    @Composable
    fun getLauncher(onResult: (Boolean) -> Unit): ManagedActivityResultLauncher<String, Boolean>

    /**
     * Method for checking whether the permission is granted.
     */
    fun checkPermission(permission: String): Boolean

    /**
     * Method for checking if an informative UI should be shown the user.
     */
    fun shouldShouldRequestPermissionRationale(
        permission: String,
    ): Boolean
}
