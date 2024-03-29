package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model

import com.x8bit.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem

data class ItemListingData(
    val alertThresholdSeconds: Int,
    val authenticatorData: List<VerificationCodeItem>?,
)
