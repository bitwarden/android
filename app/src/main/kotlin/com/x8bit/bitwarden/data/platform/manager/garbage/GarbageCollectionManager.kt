package com.x8bit.bitwarden.data.platform.manager.garbage

/**
 * A manager for interfacing with the garbage collector.
 */
interface GarbageCollectionManager {
    /**
     * Calls the garbage collector on the [Runtime] in an effort to clear the unused resources in
     * the heap.
     */
    fun tryCollect()
}
