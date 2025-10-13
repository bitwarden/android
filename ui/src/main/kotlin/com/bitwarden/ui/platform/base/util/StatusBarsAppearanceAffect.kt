package com.bitwarden.ui.platform.base.util

import android.app.Activity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle

/**
 * Sets the appearance of the StatusBars to the [isLightStatusBars] value and clears the value once
 * lifecycle is stopped.
 */
@Composable
fun StatusBarsAppearanceAffect(
    isLightStatusBars: Boolean,
    view: View = LocalView.current,
) {
    if (view.isInEditMode) return
    val activity = view.context as Activity
    val insetsController = WindowCompat.getInsetsController(activity.window, view)
    val originalStatusBarValue = insetsController.isAppearanceLightStatusBars
    LifecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                insetsController.isAppearanceLightStatusBars = isLightStatusBars
            }

            Lifecycle.Event.ON_STOP -> {
                insetsController.isAppearanceLightStatusBars = originalStatusBarValue
            }

            Lifecycle.Event.ON_CREATE,
            Lifecycle.Event.ON_RESUME,
            Lifecycle.Event.ON_PAUSE,
            Lifecycle.Event.ON_DESTROY,
            Lifecycle.Event.ON_ANY,
                -> Unit
        }
    }
}
