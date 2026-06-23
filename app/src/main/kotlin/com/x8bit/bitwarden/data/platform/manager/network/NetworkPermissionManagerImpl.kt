package com.x8bit.bitwarden.data.platform.manager.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The default implementation of [NetworkPermissionManager].
 */
internal class NetworkPermissionManagerImpl(
    private val context: Context,
    private val resourceManager: ResourceManager,
) : NetworkPermissionManager {
    private val mutableIsLocalNetworkAccessRequiredStateFlow = MutableStateFlow(value = false)

    override val errorMessageString: String
        get() = resourceManager.getString(
            resId = BitwardenString
                .your_request_was_interrupted_because_the_app_needs_local_network_access,
        )

    override val hasLocalNetworkAccessPermission: Boolean
        get() = if (isBuildVersionAtLeast(version = Build.VERSION_CODES.CINNAMON_BUN)) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_LOCAL_NETWORK,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    override val isLocalNetworkAccessRequiredStateFlow: StateFlow<Boolean> =
        mutableIsLocalNetworkAccessRequiredStateFlow

    override fun acquireLocalNetworkAccessPermission() {
        mutableIsLocalNetworkAccessRequiredStateFlow.value = true
    }

    override fun clearIsLocalNetworkAccessRequired() {
        mutableIsLocalNetworkAccessRequiredStateFlow.value = false
    }
}
