package com.x8bit.bitwarden.data.generator.repository

import com.bitwarden.core.PasswordGeneratorRequest
import com.x8bit.bitwarden.data.generator.datasource.sdk.GeneratorSdkSource
import com.x8bit.bitwarden.data.generator.repository.model.GeneratedPasswordResult
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GeneratorRepositoryTest {

    private val generatorSdkSource: GeneratorSdkSource = mockk()

    private val repository = GeneratorRepositoryImpl(
        generatorSdkSource = generatorSdkSource,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(generatorSdkSource)
    }

    @Test
    fun `generatePassword should emit Success result with the generated password`() = runTest {
        val request = PasswordGeneratorRequest(
            lowercase = true,
            uppercase = true,
            numbers = true,
            special = true,
            length = 12.toUByte(),
            avoidAmbiguous = false,
            minLowercase = null,
            minUppercase = null,
            minNumber = null,
            minSpecial = null,
        )
        val expectedResult = "GeneratedPassword123!"
        coEvery {
            generatorSdkSource.generatePassword(request)
        } returns Result.success(expectedResult)

        val result = repository.generatePassword(request)

        assertEquals(expectedResult, (result as GeneratedPasswordResult.Success).generatedString)
        coVerify { generatorSdkSource.generatePassword(request) }
    }

    @Test
    fun `generatePassword should emit InvalidRequest result when SDK throws exception`() = runTest {
        val request = PasswordGeneratorRequest(
            lowercase = true,
            uppercase = true,
            numbers = true,
            special = true,
            length = 12.toUByte(),
            avoidAmbiguous = false,
            minLowercase = null,
            minUppercase = null,
            minNumber = null,
            minSpecial = null,
        )
        val exception = RuntimeException("An error occurred")
        coEvery { generatorSdkSource.generatePassword(request) } returns Result.failure(exception)

        val result = repository.generatePassword(request)

        assertTrue(result is GeneratedPasswordResult.InvalidRequest)
        coVerify { generatorSdkSource.generatePassword(request) }
    }
}
