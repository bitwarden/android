package com.x8bit.bitwarden.ui.platform.base.util

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
     * * The value for whether a rationale should be shown to the user.
     */
    var shouldShowRequestRationale: Boolean = false

    @Composable
    override fun getLauncher(
        onResult: (Boolean) -> Unit,
    ): ManagedActivityResultLauncher<String, Boolean> {
        return mockk {
            every { launch(any()) } answers { onResult.invoke(getPermissionsResult) }
        }
    }

    override fun checkPermission(permission: String): Boolean {
        return checkPermissionResult
    }

    override fun shouldShouldRequestPermissionRationale(
        permission: String,
    ): Boolean {
       return shouldShowRequestRationale
    }
}
