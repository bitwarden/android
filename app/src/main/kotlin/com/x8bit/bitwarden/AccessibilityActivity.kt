package com.x8bit.bitwarden

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.util.validate

/**
 * An activity to be launched and then immediately closed so that the OS Shade can be collapsed
 * after the user clicks on the Autofill Quick Tile.
 */
@OmitFromCoverage
class AccessibilityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        intent = intent.validate()
        super.onCreate(savedInstanceState)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent.validate())
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent.validate(), caller)
    }
}
