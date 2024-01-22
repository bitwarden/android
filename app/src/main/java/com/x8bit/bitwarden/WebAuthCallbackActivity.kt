package com.x8bit.bitwarden

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * An activity to receive callbacks from Custom Chrome tabs or other web-auth related flows such
 * the current state of the task holding the [MainActivity] can remain undisturbed.
 */
@AndroidEntryPoint
class WebAuthCallbackActivity : AppCompatActivity() {

    private val webAuthCallbackViewModel: WebAuthCallbackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webAuthCallbackViewModel.trySendAction(
            WebAuthCallbackAction.IntentReceive(intent = intent),
        )

        val intent = Intent(this, MainActivity::class.java)
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        startActivity(intent)
        finishAffinity()
    }
}
