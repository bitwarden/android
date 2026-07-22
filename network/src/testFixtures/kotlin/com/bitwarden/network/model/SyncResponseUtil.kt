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
    policies: List<SyncResponseJson.Policy>? = listOf(createMockPolicy(number = number)),
    domains: SyncResponseJson.Domains = createMockDomains(number = number),
    sends: List<SyncResponseJson.Send> = listOf(createMockSend(number = number)),
    userDecryption: UserDecryptionJson? = createMockUserDecryption(number = number),
): SyncResponseJson =
    SyncResponseJson(
        folders = folders,
        collections = collections,
        profile = profile,
        ciphers = ciphers,
        legacyPolicies = policies,
        newPolicies = policies,
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
    v2UpgradeToken: V2UpgradeTokenJson? = createMockV2UpgradeToken(number = number),
): UserDecryptionJson =
    UserDecryptionJson(
        masterPasswordUnlock = masterPasswordUnlock,
        v2UpgradeToken = v2UpgradeToken,
    )

/**
 * Create a mock [V2UpgradeTokenJson] with a given [number].
 */
fun createMockV2UpgradeToken(
    number: Int,
    wrappedUserKey1: String = "mockWrappedUserKey1-$number",
    wrappedUserKey2: String = "mockWrappedUserKey2-$number",
): V2UpgradeTokenJson =
    V2UpgradeTokenJson(
        wrappedUserKey1 = wrappedUserKey1,
        wrappedUserKey2 = wrappedUserKey2,
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
