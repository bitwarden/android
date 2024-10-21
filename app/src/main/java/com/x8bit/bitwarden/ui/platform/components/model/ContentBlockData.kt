package com.x8bit.bitwarden.ui.platform.components.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString

/**
 * Wrapper class for data to display in a
 * [com.x8bit.bitwarden.ui.platform.components.content.BitwardenContentBlock]
 */
@Immutable
data class ContentBlockData(
    val headerText: AnnotatedString,
    val subtitleText: String? = null,
    @DrawableRes val iconResource: Int? = null,
) {
    /**
     * Overloaded constructor for [ContentBlockData] that takes a [String] for the
     * header text.
     */
    constructor(
        headerText: String,
        subtitleText: String? = null,
        iconResource: Int? = null,
    ) : this(
        headerText = AnnotatedString(headerText),
        subtitleText = subtitleText,
        iconResource = iconResource,
    )
}
