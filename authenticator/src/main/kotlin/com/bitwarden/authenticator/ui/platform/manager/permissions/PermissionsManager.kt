package com.bitwarden.authenticator.ui.platform.manager.permissions

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
     * Method for creating and returning a permissions launcher that can request multiple
     * permissions at once.
     */
    @Composable
    fun getPermissionsLauncher(
        onResult: (Map<String, Boolean>) -> Unit,
    ): ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>

    /**
     * Method for checking whether the permission is granted.
     */
    fun checkPermission(permission: String): Boolean

    /**
     * Method for checking whether the permissions are granted. This returns `true` only if all
     * permissions have been granted, `false` otherwise.
     */
    fun checkPermissions(permissions: Array<String>): Boolean

    /**
     * Method for checking if an informative UI should be shown the user.
     */
    fun shouldShouldRequestPermissionRationale(
        permission: String,
    ): Boolean
}
