package com.x8bit.bitwarden.data.platform.manager.garbage

import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Default implementation of the [GarbageCollectionManager].
 */
@Suppress("ExplicitGarbageCollectionCall")
class GarbageCollectionManagerImpl(
    private val garbageCollector: () -> Unit = { Runtime.getRuntime().gc() },
    dispatcherManager: DispatcherManager,
) : GarbageCollectionManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private var collectionJob: Job = Job().apply { complete() }

    override fun tryCollect() {
        collectionJob.cancel()
        collectionJob = unconfinedScope.launch {
            delay(timeMillis = GARBAGE_COLLECTION_INITIAL_DELAY_MS)
            repeat(times = GARBAGE_COLLECTION_ATTEMPTS) {
                delay(timeMillis = GARBAGE_COLLECTION_BASE_BACKOFF_MS * it)
                garbageCollector()
            }
        }
    }
}

/**
 * The number of time the garbage collector should be called.
 */
private const val GARBAGE_COLLECTION_ATTEMPTS: Int = 10

/**
 * The base delay, in milliseconds, between a garbage collection attempt. The duration will be
 * multiplied by the number of attempts made thus far.
 */
private const val GARBAGE_COLLECTION_BASE_BACKOFF_MS: Long = 100L

/**
 * The initial delay, in milliseconds, before the first garbage collection attempt.
 */
private const val GARBAGE_COLLECTION_INITIAL_DELAY_MS: Long = 100L
