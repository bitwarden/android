package com.x8bit.bitwarden.data.platform.manager

import android.content.Context

/**
 * F-Droid implementation of [GmsManager]. Always returns `false` since GMS is not available.
 */
@Suppress("UnusedParameter")
class GmsManagerImpl(
    context: Context,
) : GmsManager {

    override fun isVersionAtLeast(version: Int): Boolean = false
}
