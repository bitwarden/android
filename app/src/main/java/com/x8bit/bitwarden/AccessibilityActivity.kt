package com.x8bit.bitwarden

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * An activity to be launched and then immediately closed so that the OS Shade can be collapsed
 * after the user clicks on the Autofill Quick Tile.
 */
@OmitFromCoverage
class AccessibilityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
