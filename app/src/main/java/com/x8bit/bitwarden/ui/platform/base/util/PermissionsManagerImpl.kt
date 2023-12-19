package com.x8bit.bitwarden.ui.platform.base.util

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

/**
 * Primary implementation of [PermissionsManager].
 */
class PermissionsManagerImpl(
    private val activity: Activity,
) : PermissionsManager {

    @Composable
    override fun getLauncher(
        onResult: (Boolean) -> Unit,
    ): ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult,
        )

    override fun checkPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            activity,
            permission,
        ) == PackageManager.PERMISSION_GRANTED

    override fun shouldShouldRequestPermissionRationale(
        permission: String,
    ): Boolean =
        activity.shouldShowRequestPermissionRationale(permission)
}
