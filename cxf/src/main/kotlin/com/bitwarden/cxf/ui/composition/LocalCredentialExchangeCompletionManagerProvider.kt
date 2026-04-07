@file:OmitFromCoverage

package com.bitwarden.cxf.ui.composition

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManager

/**
 * Provides access to the Credential Exchange completion manager throughout the app.
 */
@Suppress("MaxLineLength")
val LocalCredentialExchangeCompletionManager: ProvidableCompositionLocal<CredentialExchangeCompletionManager> =
    compositionLocalOf {
        error("CompositionLocal LocalCredentialExchangeCompletionManager not present")
    }
