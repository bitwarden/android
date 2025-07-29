package com.x8bit.bitwarden.ui.platform.manager.intent

import com.bitwarden.annotation.OmitFromCoverage

/**
 * The default implementation of the [IntentManager] for simplifying the handling of Android
 * Intents within a given context.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@OmitFromCoverage
class IntentManagerImpl(
    activityResultIntentManager: ActivityResultIntentManager,
    applicationDataIntentManager: ApplicationDataIntentManager,
    autofillIntentManager: AutofillIntentManager,
    credentialManagerIntentManager: CredentialManagerIntentManager,
    externalLinkIntentManager: ExternalLinkIntentManager,
    fileIntentManager: FileIntentManager,
    shareIntentManager: ShareIntentManager,
    tileIntentManager: TileIntentManager,
) :
    ActivityResultIntentManager by activityResultIntentManager,
    ApplicationDataIntentManager by applicationDataIntentManager,
    AutofillIntentManager by autofillIntentManager,
    CredentialManagerIntentManager by credentialManagerIntentManager,
    ExternalLinkIntentManager by externalLinkIntentManager,
    FileIntentManager by fileIntentManager,
    ShareIntentManager by shareIntentManager,
    TileIntentManager by tileIntentManager
