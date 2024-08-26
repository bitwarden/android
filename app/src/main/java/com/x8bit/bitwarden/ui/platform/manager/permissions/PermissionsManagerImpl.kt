package com.x8bit.bitwarden.ui.platform.manager.permissions

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Primary implementation of [PermissionsManager].
 */
@OmitFromCoverage
class PermissionsManagerImpl(
    private val activity: Activity,
) : PermissionsManager {

    @Composable
    override fun getLauncher(
        onResult: (Boolean) -> Unit,
    ): ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = onResult,
        )

    @Composable
    override fun getPermissionsLauncher(
        onResult: (Map<String, Boolean>) -> Unit,
    ): ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = onResult,
        )

    override fun checkPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            activity,
            permission,
        ) == PackageManager.PERMISSION_GRANTED

    override fun checkPermissions(permissions: Array<String>): Boolean =
        permissions.map { checkPermission(it) }.all { isGranted -> isGranted }

    override fun shouldShowRequestPermissionRationale(
        permission: String,
    ): Boolean =
        activity.shouldShowRequestPermissionRationale(permission)
}
