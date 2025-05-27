package com.x8bit.bitwarden.data.tools.generator.repository.utils

import com.bitwarden.generators.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult

/**
 * Generates a random string of length [length] using [GeneratorRepository.generatePassword].
 */
suspend fun GeneratorRepository.generateRandomString(length: Int): String {
    return this.generatePassword(
        passwordGeneratorRequest = PasswordGeneratorRequest(
            lowercase = true,
            uppercase = true,
            numbers = true,
            special = false,
            length = length.toUByte(),
            avoidAmbiguous = false,
            minLowercase = null,
            minUppercase = null,
            minNumber = null,
            minSpecial = null,
        ),
        shouldSave = false,
    )
        .let {
            when (it) {
                is GeneratedPasswordResult.InvalidRequest -> throw IllegalArgumentException()
                is GeneratedPasswordResult.Success -> it.generatedString
            }
        }
}
