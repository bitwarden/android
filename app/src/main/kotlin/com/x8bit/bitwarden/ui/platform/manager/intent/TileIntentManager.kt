package com.x8bit.bitwarden.ui.platform.manager.intent

import android.app.PendingIntent
import android.content.Intent

/**
 * A manager interface for handling intents related to quick settings tiles.
 */
interface TileIntentManager {
    /**
     * Creates an intent using [data] when selecting a quick settings tile.
     */
    fun createTileIntent(data: String): Intent

    /**
     * Creates a pending intent using [requestCode] and [tileIntent] when selecting a quick
     * settings tile on API 34+.
     */
    fun createTilePendingIntent(requestCode: Int, tileIntent: Intent): PendingIntent
}
