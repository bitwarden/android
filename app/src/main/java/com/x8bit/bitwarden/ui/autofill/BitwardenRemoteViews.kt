package com.x8bit.bitwarden.ui.autofill

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
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
    buildAutofillRemoteViews(
        autofillAppInfo = autofillAppInfo,
        name = autofillCipher.name,
        subtitle = autofillCipher.subtitle,
        iconRes = autofillCipher.iconRes,
        shouldTintIcon = true,
    )

/**
 * Build [RemoteViews] to represent the Vault Item suggestion (for opening or unlocking the vault).
 */
fun buildVaultItemAutofillRemoteViews(
    autofillAppInfo: AutofillAppInfo,
    isLocked: Boolean,
): RemoteViews =
    buildAutofillRemoteViews(
        autofillAppInfo = autofillAppInfo,
        name = autofillAppInfo.context.getString(R.string.app_name),
        subtitle = autofillAppInfo.context.run {
            if (isLocked) {
                getString(R.string.vault_is_locked)
            } else {
                getString(R.string.go_to_my_vault)
            }
        },
        iconRes = R.drawable.icon,
        shouldTintIcon = false,
    )

private fun buildAutofillRemoteViews(
    autofillAppInfo: AutofillAppInfo,
    name: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    shouldTintIcon: Boolean,
): RemoteViews =
    RemoteViews(
        autofillAppInfo.packageName,
        R.layout.autofill_remote_view,
    )
        .apply {
            setTextViewText(
                R.id.title,
                name,
            )
            setTextViewText(
                R.id.subtitle,
                subtitle,
            )
            setImageViewResource(
                R.id.icon,
                iconRes,
            )
            setInt(
                R.id.container,
                "setBackgroundColor",
                autofillAppInfo.context.surface,
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
            if (shouldTintIcon) {
                setInt(
                    R.id.icon,
                    "setColorFilter",
                    autofillAppInfo.context.onSurface,
                )
            }
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
