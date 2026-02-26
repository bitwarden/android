package com.x8bit.bitwarden

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.util.validate
import com.x8bit.bitwarden.data.credentials.BitwardenCredentialProviderService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Transparent trampoline activity for handling credential provider operations.
 *
 * This activity is declared as `exported="false"` in the manifest to ensure only
 * our own PendingIntents can launch it. This protects against external apps attempting
 * to extract vault credentials by sending malicious intents via CredentialManager.
 *
 * All credential flows (FIDO2 passkeys, password credentials) are routed through this
 * activity when triggered by the Android CredentialManager framework via our
 * [BitwardenCredentialProviderService].
 *
 * ## Architecture
 *
 * This activity does not host any UI itself. It acts as a trampoline that:
 * 1. Receives the credential intent from the CredentialManager framework
 * 2. Sets the pending credential request via [CredentialProviderViewModel], which stores
 *    it in `CredentialProviderRequestManager` for secure relay to [MainViewModel]
 * 3. Launches [MainActivity] to handle the actual credential UI
 * 4. Forwards the result back to the CredentialManager framework
 *
 * This preserves the single-Activity architecture where all UI is hosted by MainActivity,
 * while still allowing the CredentialManager framework to receive results properly.
 */
@OmitFromCoverage
@AndroidEntryPoint
class CredentialProviderActivity : ComponentActivity() {

    private val viewModel: CredentialProviderViewModel by viewModels()

    /**
     * Launcher for MainActivity that forwards the result back to Credential Manager.
     */
    private val mainActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        // Forward result back to Credential Manager framework
        setResult(result.resultCode, result.data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        intent = intent.validate()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            // Process credential intent (sets pending request on CredentialProviderRequestManager)
            viewModel.trySendAction(CredentialProviderAction.ReceiveFirstIntent(intent))
            launchMainActivityForResult()
        }
        // On restoration (process death), result comes via mainActivityLauncher callback
    }

    private fun launchMainActivityForResult() {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            // Pending credential request is retrieved by MainViewModel from
            // CredentialProviderRequestManager, triggering appropriate navigation.
            // CredentialProviderCompletionManager handles setResult/finish.
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        mainActivityLauncher.launch(mainIntent)
    }

    override fun onNewIntent(intent: Intent) {
        val newIntent = intent.validate()
        super.onNewIntent(newIntent)
        viewModel.trySendAction(CredentialProviderAction.ReceiveNewIntent(newIntent))
        launchMainActivityForResult()
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        val newIntent = intent.validate()
        super.onNewIntent(newIntent, caller)
        viewModel.trySendAction(CredentialProviderAction.ReceiveNewIntent(newIntent))
        launchMainActivityForResult()
    }
}
