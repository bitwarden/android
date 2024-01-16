package com.x8bit.bitwarden.ui.autofill

import android.content.Context
import android.widget.RemoteViews
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.ui.autofill.util.isSystemDarkMode

/**
 * Build [RemoteViews] for representing an autofill suggestion.
 */
fun buildAutofillRemoteViews(
    autofillAppInfo: AutofillAppInfo,
    autofillCipher: AutofillCipher,
): RemoteViews =
    RemoteViews(
        autofillAppInfo.packageName,
        R.layout.autofill_remote_view,
    )
        .apply {
            setTextViewText(
                R.id.title,
                autofillCipher.name,
            )
            setTextViewText(
                R.id.subtitle,
                autofillCipher.subtitle,
            )
            setImageViewResource(
                R.id.icon,
                autofillCipher.iconRes,
            )

            setInt(
                R.id.container,
                "setBackgroundColor",
                autofillAppInfo.context.surface,
            )
            setInt(
                R.id.icon,
                "setColorFilter",
                autofillAppInfo.context.onSurface,
            )
            setInt(
                R.id.title,
                "setTextColor",
                autofillAppInfo.context.onSurface,
            )
            setInt(
                R.id.subtitle,
                "setTextColor",
                autofillAppInfo.context.onSurfaceVariant,
            )
        }

private val Context.onSurface: Int
    get() = getColor(
        if (isSystemDarkMode) R.color.dark_on_surface else R.color.on_surface,
    )
private val Context.onSurfaceVariant: Int
    get() = getColor(
        if (isSystemDarkMode) R.color.dark_on_surface_variant else R.color.on_surface_variant,
    )

private val Context.surface: Int
    get() = getColor(
        if (isSystemDarkMode) R.color.dark_surface else R.color.surface,
    )
