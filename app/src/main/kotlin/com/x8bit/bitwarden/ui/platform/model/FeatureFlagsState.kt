package com.x8bit.bitwarden.ui.platform.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * Models the UI centric feature flags.
 */
@Immutable
@Parcelize
data object FeatureFlagsState : Parcelable
