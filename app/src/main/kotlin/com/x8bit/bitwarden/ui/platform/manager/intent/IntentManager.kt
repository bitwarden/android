package com.x8bit.bitwarden.ui.platform.manager.intent

import androidx.compose.runtime.Immutable

/**
 * A manager class for simplifying the handling of Android Intents within a given context.
 */
@Suppress("TooManyFunctions")
@Immutable
interface IntentManager :
    ActivityResultIntentManager,
    ApplicationDataIntentManager,
    AutofillIntentManager,
    CredentialManagerIntentManager,
    ExternalLinkIntentManager,
    FileIntentManager,
    ShareIntentManager,
    TileIntentManager
