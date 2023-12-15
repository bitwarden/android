package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

class FakeCiphersDao : CiphersDao {

    val storedCiphers = mutableListOf<CipherEntity>()

    var deleteCiphersCalled: Boolean = false

    private val ciphersFlow = MutableSharedFlow<List<CipherEntity>>(
        replay = 1,
        extraBufferCapacity = Int.MAX_VALUE,
    )

    init {
        ciphersFlow.tryEmit(emptyList())
    }

    override suspend fun deleteAllCiphers(userId: String) {
        deleteCiphersCalled = true
        storedCiphers.removeAll { it.userId == userId }
        ciphersFlow.tryEmit(storedCiphers.toList())
    }

    override fun getAllCiphers(userId: String): Flow<List<CipherEntity>> =
        ciphersFlow.map { ciphers -> ciphers.filter { it.userId == userId } }

    override suspend fun insertCiphers(ciphers: List<CipherEntity>) {
        storedCiphers.addAll(ciphers)
        ciphersFlow.tryEmit(ciphers.toList())
    }

    override suspend fun replaceAllCiphers(userId: String, ciphers: List<CipherEntity>) {
        storedCiphers.removeAll { it.userId == userId }
        storedCiphers.addAll(ciphers)
        ciphersFlow.tryEmit(ciphers.toList())
    }
}
