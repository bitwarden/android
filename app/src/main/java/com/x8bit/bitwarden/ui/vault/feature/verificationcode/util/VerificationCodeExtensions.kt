package com.x8bit.bitwarden.ui.vault.feature.verificationcode.util

import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.VerificationCodeDisplayItem
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.VerificationCodeState

/**
 * Converts a list of [CipherView] to a list of [VerificationCodeDisplayItem].
 */
fun List<CipherView>.toVerificationCodeViewState(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): VerificationCodeState.ViewState =
    if (isNotEmpty()) {
        VerificationCodeState.ViewState.Content(
            verificationCodeDisplayItems = toDisplayItemList(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
            ),
        )
    } else {
        VerificationCodeState.ViewState.NoItems
    }

private fun List<CipherView>.toDisplayItemList(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): List<VerificationCodeDisplayItem> =
    this.map {
        it.toDisplayItem(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
        )
    }

/**
 *  A function used to create a sample [VerificationCodeDisplayItem].
 */
fun CipherView.toDisplayItem(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): VerificationCodeDisplayItem =
    VerificationCodeDisplayItem(
        id = id.orEmpty(),
        authCode = "123456",
        label = name,
        supportingLabel = login?.username,
        periodSeconds = 30,
        timeLeftSeconds = 15,
        startIcon = login?.uris.toLoginIconData(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
        ),
    )
