package com.x8bit.bitwarden.ui.platform.composition.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.x8bit.bitwarden.ui.platform.composition.LocalFeatureFlagsState

/**
 * Returns [new] when the `vfo1-foundation` feature flag is enabled, otherwise returns [old].
 *
 * This is a temporary helper used to gate the VFO-1 terminology rename work; it should be
 * removed, along with all of its call sites, once the flag is fully rolled out.
 */
@Composable
@ReadOnlyComposable
fun <T> vfo1Foundation(new: T, old: T): T =
    if (LocalFeatureFlagsState.current.isVfo1FoundationEnabled) new else old
