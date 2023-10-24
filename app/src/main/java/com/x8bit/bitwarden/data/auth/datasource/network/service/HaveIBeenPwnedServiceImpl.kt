package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.HaveIBeenPwnedApi
import java.security.MessageDigest

class HaveIBeenPwnedServiceImpl(private val api: HaveIBeenPwnedApi) : HaveIBeenPwnedService {

    @Suppress("MagicNumber")
    override suspend fun hasPasswordBeenBreached(password: String): Result<Boolean> {
        // Hash the password:
        val hashedPassword = MessageDigest
            .getInstance("SHA-1")
            .digest(password.toByteArray())
            .joinToString(separator = "", transform = { "%02x".format(it) })
        // Take just the prefix to send to the API:
        val hashPrefix = hashedPassword.substring(0, 5)

        return api
            .fetchBreachedPasswords(hashPrefix = hashPrefix)
            .mapCatching { responseBody ->
                val allPwnedPasswords = responseBody.string()
                    // First split the response by newline: each hashed password is on a new line.
                    .split("\r\n")
                    .map { pwnedSuffix ->
                        // Then remove everything after the ":", since we only want the pwned hash:
                        // Before: 20d61603aba324bf08799896110561f05e1ad3be:12
                        // After: 20d61603aba324bf08799896110561f05e1ad3be
                        pwnedSuffix.substring(0, endIndex = pwnedSuffix.indexOf(":"))
                    }
                // Then see if any of those passwords match our full password hash:
                allPwnedPasswords.any { pwnedSuffix ->
                    (hashPrefix + pwnedSuffix).equals(hashedPassword, ignoreCase = true)
                }
            }
    }
}
