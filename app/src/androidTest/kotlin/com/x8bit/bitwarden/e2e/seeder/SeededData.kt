package com.x8bit.bitwarden.e2e.seeder

import java.util.UUID

/**
 * A user created through the Seeder.
 *
 * @property email The address stored by the server (mangled); the one to log in with.
 * @property password Master password.
 * @property userId The user's ID.
 * @property userKeyB64 The user's symmetric key, used to seed their vault items.
 */
class SeededAccount(
    val email: String,
    val password: String,
    val userId: String,
    val userKeyB64: String,
)

/**
 * A login item to seed for a user. Plain values; the Seeder encrypts them server-side.
 *
 * @property name Item name.
 * @property username Login username.
 * @property password Login password.
 * @property uri Login URI.
 */
data class LoginItem(
    val name: String,
    val username: String,
    val password: String,
    val uri: String,
)

/**
 * Seeds a Free account on the self-hosted server under test and returns its credentials.
 */
fun SeederClient.createFreeUser(): SeededAccount {
    // Unique even before mangling, so reruns and parallel runs never collide.
    val originalEmail = "native-e2e-${UUID.randomUUID()}@example.com"
    val password = "Native-E2E-${UUID.randomUUID()}"
    val response = seed(
        template = "SingleUserScene",
        arguments = SingleUserSceneArguments(
            email = originalEmail,
            password = password,
            emailVerified = true,
            selfHosted = true,
            kdfIterations = KDF_ITERATIONS,
        ),
        argumentsSerializer = SingleUserSceneArguments.serializer(),
        resultSerializer = SingleUserSceneResult.serializer(),
    )
    // Mangling and play-id tracking are enabled together on the Seeder; without them the
    // cleanup in BaseE2eTest would silently delete nothing.
    val email = requireNotNull(response.mangleMap[originalEmail]) {
        "Seeder returned no mangled email; play-id tracking is off, so seeded data can't be " +
            "cleaned up."
    }
    return SeededAccount(
        email = email,
        password = password,
        userId = response.result.userId,
        userKeyB64 = response.result.decryptedKeyB64,
    )
}

/**
 * Seeds [item] into [account]'s personal vault.
 */
fun SeederClient.addLoginItem(account: SeededAccount, item: LoginItem) {
    seed(
        template = "UserLoginCipherScene",
        arguments = UserLoginCipherSceneArguments(
            userId = account.userId,
            userKeyB64 = account.userKeyB64,
            name = item.name,
            username = item.username,
            password = item.password,
            uri = item.uri,
        ),
        argumentsSerializer = UserLoginCipherSceneArguments.serializer(),
        resultSerializer = UserLoginCipherSceneResult.serializer(),
    )
}

// Matches the C# suite's production-realistic default; lower values trigger the app's KDF prompt.
private const val KDF_ITERATIONS = 600_000
