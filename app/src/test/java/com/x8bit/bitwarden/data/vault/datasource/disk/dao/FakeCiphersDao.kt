package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeCiphersDao : CiphersDao {

    val storedCiphers = mutableListOf<CipherEntity>()

    var deleteCipherCalled: Boolean = false
    var deleteCiphersCalled: Boolean = false
    var insertCiphersCalled: Boolean = false

    private val ciphersFlow = bufferedMutableSharedFlow<List<CipherEntity>>(replay = 1)

    init {
        ciphersFlow.tryEmit(emptyList())
    }

    override suspend fun deleteAllCiphers(userId: String): Int {
        deleteCiphersCalled = true
        val count = storedCiphers.count { it.userId == userId }
        storedCiphers.removeAll { it.userId == userId }
        ciphersFlow.tryEmit(storedCiphers.toList())
        return count
    }

    override suspend fun deleteCipher(userId: String, cipherId: String): Int {
        deleteCipherCalled = true
        val count = storedCiphers.count { it.userId == userId && it.id == cipherId }
        storedCiphers.removeAll { it.userId == userId && it.id == cipherId }
        ciphersFlow.tryEmit(storedCiphers.toList())
        return count
    }

    override fun getAllCiphers(userId: String): Flow<List<CipherEntity>> =
        ciphersFlow.map { ciphers -> ciphers.filter { it.userId == userId } }

    override suspend fun insertCiphers(ciphers: List<CipherEntity>) {
        storedCiphers.addAll(ciphers)
        ciphersFlow.tryEmit(ciphers.toList())
        insertCiphersCalled = true
    }

    override suspend fun replaceAllCiphers(userId: String, ciphers: List<CipherEntity>): Boolean {
        val removed = storedCiphers.removeAll { it.userId == userId }
        storedCiphers.addAll(ciphers)
        ciphersFlow.tryEmit(ciphers.toList())
        return removed || ciphers.isNotEmpty()
    }
}
