package com.x8bit.bitwarden.data.tools.generator.repository.util

import com.bitwarden.generators.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.model.GeneratedPasswordResult
import com.x8bit.bitwarden.data.tools.generator.repository.utils.generateRandomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GeneratorRepositoryExtensionsTest {
    private val generatorRepository: GeneratorRepository = mockk {
        coEvery {
            generatePassword(any(), any())
        } returns GeneratedPasswordResult.Success("abc")
    }

    @Test
    fun `generateRandomString should call generatePassword with the correct parameters`() =
        runTest {
            generatorRepository.generateRandomString(64)

            coVerify(exactly = 1) {
                generatorRepository.generatePassword(
                    passwordGeneratorRequest = PasswordGeneratorRequest(
                        lowercase = true,
                        uppercase = true,
                        numbers = true,
                        special = false,
                        length = 64.toUByte(),
                        avoidAmbiguous = false,
                        minLowercase = null,
                        minUppercase = null,
                        minNumber = null,
                        minSpecial = null,
                    ),
                    shouldSave = false,
                )
            }
        }
}
