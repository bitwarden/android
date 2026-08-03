package com.x8bit.bitwarden.ui.platform.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * Models the UI centric feature flags.
 *
 * @property isVfo1FoundationEnabled Whether the VFO-1 naming (terminology rename) work is
 * enabled.
 */
@Immutable
@Parcelize
data class FeatureFlagsState(
    val isVfo1FoundationEnabled: Boolean = false,
) : Parcelable
