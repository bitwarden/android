package com.bitwarden.ui.platform.base.util

import android.app.Activity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Sets the appearance of the StatusBars to the [isLightStatusBars] value and clears the value once
 * disposed.
 */
@Composable
fun StatusBarsAppearanceAffect(
    isLightStatusBars: Boolean,
    view: View = LocalView.current,
) {
    if (view.isInEditMode) return
    val activity = view.context as Activity
    DisposableEffect(Unit) {
        val insetsController = WindowCompat.getInsetsController(activity.window, view)
        val originalStatusBarValue = insetsController.isAppearanceLightStatusBars
        insetsController.isAppearanceLightStatusBars = isLightStatusBars

        onDispose {
            insetsController.isAppearanceLightStatusBars = originalStatusBarValue
        }
    }
}
