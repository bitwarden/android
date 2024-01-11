package com.x8bit.bitwarden.ui.autofill

import android.widget.RemoteViews
import com.x8bit.bitwarden.R

/**
 * Build [RemoteViews] for representing an autofill suggestion.
 */
fun buildAutofillRemoteViews(
    packageName: String,
    title: String,
): RemoteViews =
    RemoteViews(
        packageName,
        R.layout.autofill_remote_view,
    )
        .apply {
            setTextViewText(
                R.id.text,
                title,
            )
        }
