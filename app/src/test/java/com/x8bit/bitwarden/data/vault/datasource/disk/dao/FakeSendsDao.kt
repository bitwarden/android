package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.SendEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeSendsDao : SendsDao {

    val storedSends = mutableListOf<SendEntity>()

    var deleteSendsCalled: Boolean = false
    var deleteSendCalled: Boolean = false
    var insertSendsCalled: Boolean = false

    private val sendsFlow = bufferedMutableSharedFlow<List<SendEntity>>(replay = 1)

    init {
        sendsFlow.tryEmit(emptyList())
    }

    override suspend fun deleteAllSends(userId: String): Int {
        deleteSendsCalled = true
        val count = storedSends.count { it.userId == userId }
        storedSends.removeAll { it.userId == userId }
        sendsFlow.tryEmit(storedSends.toList())
        return count
    }

    override suspend fun deleteSend(userId: String, sendId: String): Int {
        deleteSendCalled = true
        val count = storedSends.count { it.userId == userId && it.id == sendId }
        storedSends.removeAll { it.userId == userId && it.id == sendId }
        sendsFlow.tryEmit(storedSends.toList())
        return count
    }

    override fun getAllSends(userId: String): Flow<List<SendEntity>> =
        sendsFlow.map { ciphers -> ciphers.filter { it.userId == userId } }

    override suspend fun insertSends(sends: List<SendEntity>) {
        storedSends.addAll(sends)
        sendsFlow.tryEmit(storedSends.toList())
        insertSendsCalled = true
    }

    override suspend fun replaceAllSends(userId: String, sends: List<SendEntity>): Boolean {
        val removed = storedSends.removeAll { it.userId == userId }
        storedSends.addAll(sends)
        sendsFlow.tryEmit(storedSends.toList())
        return removed || sends.isNotEmpty()
    }
}
