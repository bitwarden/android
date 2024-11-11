package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.HaveIBeenPwnedApi
import com.x8bit.bitwarden.data.platform.datasource.network.util.toResult
import java.security.MessageDigest

class HaveIBeenPwnedServiceImpl(private val api: HaveIBeenPwnedApi) : HaveIBeenPwnedService {

    @Suppress("MagicNumber")
    override suspend fun getPasswordBreachCount(password: String): Result<Int> {
        // Hash the password:
        val hashedPassword = MessageDigest
            .getInstance("SHA-1")
            .digest(password.toByteArray())
            .joinToString(separator = "", transform = { "%02x".format(it) })
        // Take just the prefix to send to the API:
        val hashPrefix = hashedPassword.substring(0, 5)

        return api
            .fetchBreachedPasswords(hashPrefix = hashPrefix)
            .toResult()
            .mapCatching { responseBody ->
                responseBody.string()
                    // First split the response by newline: each hashed password is on a new line.
                    .split("\r\n", "\r", "\n")
                    .associate { pwnedSuffix ->
                        // Then split everything on the ":", since we want to compare the pwned
                        // hash but we also want to return the count:
                        // Pattern: <pwnd_hash_suffix>:<breach_count>
                        // Example: 20d61603aba324bf08799896110561f05e1ad3be:12
                        val split = pwnedSuffix.split(":")
                        split[0] to split[1]
                    }
                    .entries
                    .find { (pwnedSuffix, _) ->
                        // Then see if any of those passwords match our full password hash:
                        (hashPrefix + pwnedSuffix).equals(hashedPassword, ignoreCase = true)
                    }
                    // If we found a match, return the value as this is the number of breaches.
                    ?.value
                    ?.toIntOrNull()
                    ?: 0
            }
    }

    override suspend fun hasPasswordBeenBreached(
        password: String,
    ): Result<Boolean> = getPasswordBreachCount(password)
        .map { numberOfBreaches -> numberOfBreaches > 0 }
}
