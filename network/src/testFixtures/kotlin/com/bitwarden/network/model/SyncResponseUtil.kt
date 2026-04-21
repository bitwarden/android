package com.bitwarden.network.model

/**
 * Create a mock [SyncResponseJson] with a given [number].
 */
@Suppress("LongParameterList")
fun createMockSyncResponse(
    number: Int,
    folders: List<SyncResponseJson.Folder> = listOf(createMockFolder(number = number)),
    collections: List<SyncResponseJson.Collection> = listOf(createMockCollection(number = number)),
    profile: SyncResponseJson.Profile = createMockProfile(number = number),
    ciphers: List<SyncResponseJson.Cipher> = listOf(createMockCipher(number = number)),
    policies: List<SyncResponseJson.Policy> = listOf(createMockPolicy(number = number)),
    domains: SyncResponseJson.Domains = createMockDomains(number = number),
    sends: List<SyncResponseJson.Send> = listOf(createMockSend(number = number)),
    userDecryption: UserDecryptionJson? = createMockUserDecryption(number = number),
): SyncResponseJson =
    SyncResponseJson(
        folders = folders,
        collections = collections,
        profile = profile,
        ciphers = ciphers,
        policies = policies,
        domains = domains,
        sends = sends,
        userDecryption = userDecryption,
    )

/**
 * Create a mock [UserDecryptionJson] with a given [number].
 */
fun createMockUserDecryption(
    number: Int,
    masterPasswordUnlock: MasterPasswordUnlockDataJson? = createMockMasterPasswordUnlock(
        number = number,
    ),
): UserDecryptionJson =
    UserDecryptionJson(
        masterPasswordUnlock = masterPasswordUnlock,
    )

/**
 * Create a mock [MasterPasswordUnlockDataJson] with a given [number].
 */
fun createMockMasterPasswordUnlock(
    number: Int,
    kdf: KdfJson = KdfJson(
        kdfType = KdfTypeJson.PBKDF2_SHA256,
        iterations = 600_000,
        memory = null,
        parallelism = null,
    ),
    masterKeyWrappedUserKey: String = "mockMasterKeyWrappedUserKey-$number",
    salt: String = "mockSalt-$number",
): MasterPasswordUnlockDataJson =
    MasterPasswordUnlockDataJson(
        kdf = kdf,
        masterKeyWrappedUserKey = masterKeyWrappedUserKey,
        salt = salt,
    )
