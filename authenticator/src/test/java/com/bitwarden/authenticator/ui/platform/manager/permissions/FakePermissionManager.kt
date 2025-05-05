package com.bitwarden.authenticator.ui.platform.manager.permissions

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import io.mockk.every
import io.mockk.mockk

/**
 * A helper class used to test permissions
 */
class FakePermissionManager : PermissionsManager {

    /**
     * The value returned when we check if we have the permission.
     */
    var checkPermissionResult: Boolean = false

    /**
     * The value returned when the user is asked for permission.
     */
    var getPermissionsResult: Boolean = false

    /**
     * The value returned when the user is asked for permission.
     */
    var getMultiplePermissionsResult: Map<String, Boolean> = emptyMap()

    /**
     * The value for whether a rationale should be shown to the user.
     */
    var shouldShowRequestRationale: Boolean = false

    /**
     * Indicates that the [getLauncher] function has been called.
     */
    var hasGetLauncherBeenCalled: Boolean = false

    @Composable
    override fun getLauncher(
        onResult: (Boolean) -> Unit,
    ): ManagedActivityResultLauncher<String, Boolean> {
        hasGetLauncherBeenCalled = true
        return mockk {
            every { launch(any()) } answers { onResult.invoke(getPermissionsResult) }
        }
    }

    @Composable
    override fun getPermissionsLauncher(
        onResult: (Map<String, Boolean>) -> Unit,
    ): ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> {
        return mockk {
            every { launch(any()) } answers { onResult.invoke(getMultiplePermissionsResult) }
        }
    }

    override fun checkPermission(permission: String): Boolean {
        return checkPermissionResult
    }

    override fun checkPermissions(permissions: Array<String>): Boolean {
        return checkPermissionResult
    }

    override fun shouldShouldRequestPermissionRationale(permission: String): Boolean {
        return shouldShowRequestRationale
    }
}
