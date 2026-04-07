package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailabilityLight

/**
 * Primary implementation of [GmsManager].
 */
class GmsManagerImpl(
    private val context: Context,
) : GmsManager {

    override fun isVersionAtLeast(version: Int): Boolean =
        GoogleApiAvailabilityLight.getInstance().getApkVersion(context) >= version
}
