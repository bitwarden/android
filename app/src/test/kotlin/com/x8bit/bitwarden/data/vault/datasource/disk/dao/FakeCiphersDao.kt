package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
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

    override fun getAllCiphersFlow(userId: String): Flow<List<CipherEntity>> =
        ciphersFlow.map { ciphers -> ciphers.filter { it.userId == userId } }

    override suspend fun getAllCiphers(userId: String): List<CipherEntity> =
        storedCiphers.filter { it.userId == userId }

    override suspend fun getSelectedCiphers(
        userId: String,
        cipherIds: List<String>,
    ): List<CipherEntity> =
        storedCiphers.filter { it.userId == userId && it.id in cipherIds }

    override suspend fun getAllTotpCiphers(userId: String): List<CipherEntity> =
        storedCiphers.filter { it.userId == userId && it.hasTotp }

    override suspend fun getCipher(userId: String, cipherId: String): CipherEntity? =
        storedCiphers.find { it.userId == userId && it.id == cipherId }

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

    override fun hasPersonalCiphersFlow(userId: String): Flow<Boolean> =
        ciphersFlow.map { ciphers ->
            ciphers.any { it.userId == userId && it.organizationId == null }
        }
}
