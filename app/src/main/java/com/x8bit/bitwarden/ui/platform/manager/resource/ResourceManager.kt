package com.x8bit.bitwarden.ui.platform.manager.resource

import androidx.annotation.StringRes

/**
 * Interface for managing resources.
 */
interface ResourceManager {

    /**
     * Method for returning a permission string from a [resId].
     */
    fun getString(@StringRes resId: Int): String

    /**
     * Method for returning a permission string from a [resId] with [formatArgs].
     */
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
}
