package com.x8bit.bitwarden.e2e.seeder

import kotlinx.serialization.Serializable

/**
 * SeederApi response envelope.
 *
 * @property mangleMap Original request values mapped to the unique values the Seeder stored.
 * @property result The scene's result.
 */
@Serializable
data class SeedResponse<R>(
    val mangleMap: Map<String, String> = emptyMap(),
    val result: R,
)

/**
 * Arguments for the `SingleUserScene` template.
 *
 * @property email Email before mangling.
 * @property password Master password.
 * @property emailVerified Whether the email is pre-verified.
 * @property selfHosted Whether the user is created for a self-hosted server.
 * @property kdfIterations PBKDF2 iterations for the master key.
 */
@Serializable
data class SingleUserSceneArguments(
    val email: String,
    val password: String,
    val emailVerified: Boolean,
    val selfHosted: Boolean,
    val kdfIterations: Int,
)

/**
 * Result of the `SingleUserScene` template.
 *
 * @property userId The new user's ID.
 * @property decryptedKeyB64 The user's symmetric key, used to seed their vault items.
 */
@Serializable
data class SingleUserSceneResult(
    val userId: String,
    val decryptedKeyB64: String,
)

/**
 * Arguments for the `UserLoginCipherScene` template. The Seeder encrypts the values server-side.
 *
 * @property userId Owner of the item.
 * @property userKeyB64 The owner's symmetric key.
 * @property name Item name.
 * @property username Login username.
 * @property password Login password.
 * @property uri Login URI.
 */
@Serializable
data class UserLoginCipherSceneArguments(
    val userId: String,
    val userKeyB64: String,
    val name: String,
    val username: String,
    val password: String,
    val uri: String,
)

/**
 * Result of the `UserLoginCipherScene` template.
 *
 * @property cipherId The new item's ID.
 */
@Serializable
data class UserLoginCipherSceneResult(
    val cipherId: String,
)
