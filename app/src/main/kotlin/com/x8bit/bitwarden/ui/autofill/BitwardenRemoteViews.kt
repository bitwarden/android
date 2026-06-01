package com.x8bit.bitwarden.ui.autofill

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.color.BitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.darkBitwardenColorScheme
import com.bitwarden.ui.platform.theme.color.lightBitwardenColorScheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.ui.autofill.util.getAutofillSuggestionContentDescription
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
        autofillContentDescription = getAutofillSuggestionContentDescription(
            autofillCipher = autofillCipher,
            autofillAppInfo = autofillAppInfo,
        ),
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
                getString(BitwardenString.vault_is_locked)
            } else {
                getString(BitwardenString.go_to_my_vault)
            }
        },
        iconRes = BitwardenDrawable.logo_bitwarden_icon,
        shouldTintIcon = false,
        autofillContentDescription = null,
    )

@Suppress("LongParameterList")
private fun buildAutofillRemoteViews(
    autofillAppInfo: AutofillAppInfo,
    autofillContentDescription: String?,
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
            autofillContentDescription?.let {
                setContentDescription(
                    R.id.container,
                    it,
                )
            }
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
                autofillAppInfo.context.background.toArgb(),
            )
            setInt(
                R.id.title,
                "setTextColor",
                autofillAppInfo.context.primaryText.toArgb(),
            )
            setInt(
                R.id.subtitle,
                "setTextColor",
                autofillAppInfo.context.secondaryText.toArgb(),
            )
            if (shouldTintIcon) {
                setInt(
                    R.id.icon,
                    "setColorFilter",
                    autofillAppInfo.context.iconTint.toArgb(),
                )
            }
        }

private val Context.bitwardenColorScheme: BitwardenColorScheme
    get() = if (isSystemDarkMode) darkBitwardenColorScheme else lightBitwardenColorScheme

val Context.iconTint: Color get() = bitwardenColorScheme.icon.primary
private val Context.primaryText: Color get() = bitwardenColorScheme.text.primary
private val Context.secondaryText: Color get() = bitwardenColorScheme.text.secondary
private val Context.background: Color get() = bitwardenColorScheme.background.primary
