package com.x8bit.bitwarden.ui.platform.util

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.toRoute

/**
 * Determines if the [SavedStateHandle] contains a route for the specified object class.
 *
 * This will return the object instance if the route is correct, `null` otherwise.
 */
inline fun <reified T : Any> SavedStateHandle.toObjectRoute(): T? =
    this
        .get<Intent>(key = NavController.KEY_DEEP_LINK_INTENT)
        ?.data
        ?.pathSegments
        .orEmpty()
        .takeIf { segments -> segments.any { it == T::class.toObjectKClassNavigationRoute() } }
        ?.let { _ ->
            // This will get the instance for us. We only do this after the checks above as it
            // will always return the object instance even if it is not the correct one.
            this.toRoute<T>()
        }
