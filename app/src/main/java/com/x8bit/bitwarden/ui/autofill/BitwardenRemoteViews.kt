package com.x8bit.bitwarden.ui.autofill

import android.content.Context
import android.widget.RemoteViews
import com.x8bit.bitwarden.R

/**
 * Build [RemoteViews] for representing an autofill suggestion.
 */
fun buildAutofillRemoteViews(
    context: Context,
    packageName: String,
): RemoteViews =
    RemoteViews(
        packageName,
        R.layout.autofill_remote_view,
    )
        .apply {
            setTextViewText(
                R.id.text,
                context.resources.getText(R.string.app_name),
            )
        }
