package com.bitwarden.network.model

fun createMockSyncResponse(number: Int): SyncResponseJson =
    SyncResponseJson(
        folders = listOf(createMockFolder(number = number)),
        collections = listOf(createMockCollection(number = number)),
        profile = createMockProfile(number = number),
        ciphers = listOf(createMockCipher(number = number)),
        policies = listOf(createMockPolicy(number = number)),
        domains = createMockDomains(number = number),
        sends = listOf(createMockSend(number = number)),
    )
