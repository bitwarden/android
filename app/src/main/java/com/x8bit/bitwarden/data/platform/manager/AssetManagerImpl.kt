package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.withContext

/**
 * Primary implementation of [AssetManager].
 */
@OmitFromCoverage
class AssetManagerImpl(
    private val context: Context,
    private val dispatcherManager: DispatcherManager,
) : AssetManager {

    override suspend fun readAsset(fileName: String): Result<String> = runCatching {
        withContext(dispatcherManager.io) {
            context
                .assets
                .open(fileName)
                .bufferedReader()
                .use { it.readText() }
        }
    }
}
