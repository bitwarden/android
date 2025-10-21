package com.x8bit.bitwarden.ui.autofill.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Build
import android.service.autofill.InlinePresentation
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.compose.ui.graphics.toArgb
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.ui.autofill.iconTint
import timber.log.Timber

/**
 * Try creating an [InlinePresentation] for [autofillCipher] with this [InlinePresentationSpec]. If
 * it fails, return null.
 */
@RequiresApi(Build.VERSION_CODES.R)
fun InlinePresentationSpec.createCipherInlinePresentationOrNull(
    autofillAppInfo: AutofillAppInfo,
    autofillCipher: AutofillCipher,
): InlinePresentation? =
    createInlinePresentationOrNull(
        pendingIntent = PendingIntent.getService(
            autofillAppInfo.context,
            0,
            Intent(),
            PendingIntent.FLAG_ONE_SHOT or
                PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE,
        ),
        autofillAppInfo = autofillAppInfo,
        autofillCipher = autofillCipher,
        title = autofillCipher.name,
        subtitle = autofillCipher.subtitle,
        iconRes = autofillCipher.iconRes,
        shouldTintIcon = true,
    )

/**
 * Try creating an [InlinePresentation] for the Vault item with this [InlinePresentationSpec]. If
 * it fails, return null.
 */
@RequiresApi(Build.VERSION_CODES.R)
fun InlinePresentationSpec.createVaultItemInlinePresentationOrNull(
    autofillAppInfo: AutofillAppInfo,
    pendingIntent: PendingIntent,
    isLocked: Boolean,
): InlinePresentation? =
    createInlinePresentationOrNull(
        pendingIntent = pendingIntent,
        autofillAppInfo = autofillAppInfo,
        autofillCipher = null,
        title = autofillAppInfo.context.getString(R.string.app_name),
        subtitle = if (isLocked) {
            autofillAppInfo.context.getString(BitwardenString.vault_is_locked)
        } else {
            autofillAppInfo.context.getString(BitwardenString.my_vault)
        },
        iconRes = BitwardenDrawable.logo_bitwarden_icon,
        shouldTintIcon = false,
    )

@Suppress("LongParameterList")
@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("RestrictedApi")
private fun InlinePresentationSpec.createInlinePresentationOrNull(
    pendingIntent: PendingIntent,
    autofillAppInfo: AutofillAppInfo,
    autofillCipher: AutofillCipher?,
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    shouldTintIcon: Boolean,
): InlinePresentation? {
    val isInlineCompatible = UiVersions
        .getVersions(style)
        .contains(UiVersions.INLINE_UI_VERSION_1)
    Timber.d("Autofill request isInlineCompatible=$isInlineCompatible")
    if (!isInlineCompatible) return null

    val icon = Icon
        .createWithResource(
            autofillAppInfo.context,
            iconRes,
        )
        .run {
            if (shouldTintIcon) {
                setTint(autofillAppInfo.context.iconTint.toArgb())
            } else {
                // Remove tinting
                setTintBlendMode(BlendMode.DST)
            }
        }
    val slice = InlineSuggestionUi
        .newContentBuilder(pendingIntent)
        .also { contentBuilder ->
            autofillCipher?.let {
                contentBuilder.setContentDescription(
                    getAutofillSuggestionContentDescription(
                        autofillAppInfo = autofillAppInfo,
                        autofillCipher = it,
                    ),
                )
            }
        }
        .setTitle(title)
        .setSubtitle(subtitle)
        .setStartIcon(icon)
        .build()
        .slice

    return InlinePresentation(
        slice,
        this,
        false,
    )
}
