package com.x8bit.bitwarden

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.util.validate
import dagger.hilt.android.AndroidEntryPoint

/**
 * An activity to receive external authentication-related callbacks so the current state of the
 * task holding the [MainActivity] can remain undisturbed.
 *
 * These callbacks can be from Custom Chrome tabs or other auth related flows, including NFC
 * related transmissions.
 */
@OmitFromCoverage
@AndroidEntryPoint
class AuthCallbackActivity : AppCompatActivity() {

    private val viewModel: AuthCallbackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        intent = intent.validate()
        super.onCreate(savedInstanceState)

        viewModel.trySendAction(AuthCallbackAction.IntentReceive(intent = intent))

        val intent = Intent(this, MainActivity::class.java)
            .apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
        startActivity(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent.validate())
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent.validate(), caller)
    }
}
