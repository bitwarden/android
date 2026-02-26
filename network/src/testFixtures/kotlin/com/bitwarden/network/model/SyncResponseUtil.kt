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
    userDecryption: UserDecryptionJson? = null,
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
