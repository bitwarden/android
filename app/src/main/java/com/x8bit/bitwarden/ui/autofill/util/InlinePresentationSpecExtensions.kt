package com.x8bit.bitwarden.ui.autofill.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.autofill.InlinePresentation
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher

/**
 * Try creating an [InlinePresentation] for [autofillCipher] with this [InlinePresentationSpec]. If
 * it fails, return null.
 */
@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("RestrictedApi")
fun InlinePresentationSpec.createCipherInlinePresentationOrNull(
    autofillAppInfo: AutofillAppInfo,
    autofillCipher: AutofillCipher,
): InlinePresentation? {
    val isInlineCompatible = UiVersions
        .getVersions(style)
        .contains(UiVersions.INLINE_UI_VERSION_1)

    if (!isInlineCompatible) return null

    val pendingIntent = PendingIntent.getService(
        autofillAppInfo.context,
        0,
        Intent(),
        PendingIntent.FLAG_ONE_SHOT or
            PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_IMMUTABLE,
    )
    val icon = Icon
        .createWithResource(
            autofillAppInfo.context,
            autofillCipher.iconRes,
        )
        .setTint(autofillAppInfo.contentColor)
    val slice = InlineSuggestionUi
        .newContentBuilder(pendingIntent)
        .setTitle(autofillCipher.name)
        .setSubtitle(autofillCipher.subtitle)
        .setStartIcon(icon)
        .build()
        .slice

    return InlinePresentation(
        slice,
        this,
        false,
    )
}

/**
 * Get the content color for the inline presentation.
 */
private val AutofillAppInfo.contentColor: Int
    get() {
        val colorRes = if (context.isSystemDarkMode) {
            R.color.dark_on_surface
        } else {
            R.color.on_surface
        }

        return context.getColor(colorRes)
    }
