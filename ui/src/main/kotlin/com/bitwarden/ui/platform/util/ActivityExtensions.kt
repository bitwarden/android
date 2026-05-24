@file:OmitFromCoverage

package com.bitwarden.ui.platform.util

import android.app.Activity
import android.os.Build
import com.bitwarden.annotation.OmitFromCoverage

/**
 * Requests a HorizonOS-specific window resize to 1024×640 via reflection.
 * Calls [onResizeRequested] only if the resize request succeeds.
 */
@Suppress("MagicNumber", "TooGenericExceptionCaught")
fun Activity.setHorizonOSAppLayout(
    onResizeRequested: () -> Unit,
) {
    if (!isHorizonOSDevice()) {
        return
    }
    window.decorView.post {
        try {
            val clazz = Class.forName("horizonos.view.WindowExt")
            val method = clazz.getMethod(
                "requestWindowResize",
                android.view.Window::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            method.invoke(null, window, 1024, 640)
        } catch (t: Throwable) {
            // Not Horizon OS / API not present / request ignored by system
            return@post
        }
        onResizeRequested()
    }
}

private fun isHorizonOSDevice(): Boolean =
    Build.MANUFACTURER.equals("Oculus", ignoreCase = true) ||
        Build.MANUFACTURER.equals("Meta", ignoreCase = true)
