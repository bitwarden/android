package com.x8bit.bitwarden.ui.platform.base.util

import android.content.Context
import android.content.Intent

/**
 * A utility class for simplifying the handling of Android Intents within a given context.
 */
class IntentHandler(private val context: Context) {

    /**
     * Start an activity using the provided [Intent].
     */
    fun startActivity(intent: Intent) {
        context.startActivity(intent)
    }
}
