package com.x8bit.bitwarden.ui.platform.manager.intent

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.x8bit.bitwarden.MainActivity

/**
 * Primary implementation of [TileIntentManager] for managing intents related to quick settings
 * tiles.
 */
class TileIntentManagerImpl(
    private val context: Context,
) : TileIntentManager {
    /**
     * Creates an intent using [data] when selecting a quick settings tile.
     */
    override fun createTileIntent(data: String): Intent = Intent(
        context,
        MainActivity::class.java,
    )
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .setData(data.toUri())

    /**
     * Creates a pending intent using [requestCode] and [tileIntent] when selecting a quick
     * settings tile on API 34+.
     */
    override fun createTilePendingIntent(
        requestCode: Int,
        tileIntent: Intent,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        tileIntent,
        PendingIntent.FLAG_IMMUTABLE,
    )
}
