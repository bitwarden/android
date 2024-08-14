package com.x8bit.bitwarden.data.tools.generator.datasource.disk

import androidx.core.content.edit
import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.tools.generator.repository.model.PasscodeGenerationOptions
import com.x8bit.bitwarden.data.tools.generator.repository.model.UsernameGenerationOptions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GeneratorDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val generatorDiskSource = GeneratorDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
    )

    @Test
    fun `clearData should clear all necessary data for the given user`() {
        val userId = "userId"

        val passcodeOptions = PasscodeGenerationOptions(
            type = PasscodeGenerationOptions.PasscodeType.PASSWORD,
            length = 14,
            allowAmbiguousChar = false,
            hasNumbers = true,
            minNumber = 0,
            hasUppercase = true,
            minUppercase = null,
            hasLowercase = false,
            minLowercase = null,
            allowSpecial = false,
            minSpecial = 1,
            allowCapitalize = false,
            allowIncludeNumber = false,
            wordSeparator = "-",
            numWords = 3,
        )
        val userNameOptions = UsernameGenerationOptions(
            type = UsernameGenerationOptions.UsernameType.RANDOM_WORD,
            serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.NONE,
            capitalizeRandomWordUsername = true,
            includeNumberRandomWordUsername = false,
            plusAddressedEmail = "example+plus@gmail.com",
            catchAllEmailDomain = "example.com",
            firefoxRelayApiAccessToken = "access_token_firefox_relay",
            simpleLoginApiKey = "api_key_simple_login",
            duckDuckGoApiKey = "api_key_duck_duck_go",
            fastMailApiKey = "api_key_fast_mail",
            anonAddyApiAccessToken = "access_token_anon_addy",
            anonAddyDomainName = "anonaddy.com",
            forwardEmailApiAccessToken = "access_token_forward_email",
            forwardEmailDomainName = "forwardemail.net",
            emailWebsite = "email.example.com",
        )

        generatorDiskSource.storePasscodeGenerationOptions(
            userId = userId,
            options = passcodeOptions,
        )
        generatorDiskSource.storeUsernameGenerationOptions(
            userId = userId,
            options = userNameOptions,
        )

        generatorDiskSource.clearData(userId = userId)

        assertNull(generatorDiskSource.getPasscodeGenerationOptions(userId = userId))
        assertNull(generatorDiskSource.getUsernameGenerationOptions(userId = userId))
    }

    @Test
    fun `getPasscodeGenerationOptions should return correct options when available`() {
        val userId = "user123"
        val options = PasscodeGenerationOptions(
            type = PasscodeGenerationOptions.PasscodeType.PASSWORD,
            length = 14,
            allowAmbiguousChar = false,
            hasNumbers = true,
            minNumber = 0,
            hasUppercase = true,
            minUppercase = null,
            hasLowercase = false,
            minLowercase = null,
            allowSpecial = false,
            minSpecial = 1,
            allowCapitalize = false,
            allowIncludeNumber = false,
            wordSeparator = "-",
            numWords = 3,
        )

        val key = "bwPreferencesStorage:passwordGenerationOptions_$userId"
        fakeSharedPreferences.edit { putString(key, json.encodeToString(options)) }

        val result = generatorDiskSource.getPasscodeGenerationOptions(userId)

        assertEquals(options, result)
    }

    @Test
    fun `getPasscodeGenerationOptions should return null when options are not available`() {
        val userId = "user123"

        val result = generatorDiskSource.getPasscodeGenerationOptions(userId)

        assertNull(result)
    }

    @Test
    fun `storePasscodeGenerationOptions should correctly store options`() {
        val userId = "user123"
        val options = PasscodeGenerationOptions(
            type = PasscodeGenerationOptions.PasscodeType.PASSWORD,
            length = 14,
            allowAmbiguousChar = false,
            hasNumbers = true,
            minNumber = 0,
            hasUppercase = true,
            minUppercase = null,
            hasLowercase = false,
            minLowercase = null,
            allowSpecial = false,
            minSpecial = 1,
            allowCapitalize = false,
            allowIncludeNumber = false,
            wordSeparator = "-",
            numWords = 3,
        )

        val key = "bwPreferencesStorage:passwordGenerationOptions_$userId"

        generatorDiskSource.storePasscodeGenerationOptions(userId, options)

        val storedValue = fakeSharedPreferences.getString(key, null)
        assertNotNull(storedValue)
        assertEquals(json.encodeToString(options), storedValue)
    }

    @Test
    fun `getUsernameGenerationOptions should return correct options when available`() {
        val userId = "user123"
        val options = UsernameGenerationOptions(
            type = UsernameGenerationOptions.UsernameType.RANDOM_WORD,
            serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.NONE,
            capitalizeRandomWordUsername = true,
            includeNumberRandomWordUsername = false,
            plusAddressedEmail = "example+plus@gmail.com",
            catchAllEmailDomain = "example.com",
            firefoxRelayApiAccessToken = "access_token_firefox_relay",
            simpleLoginApiKey = "api_key_simple_login",
            duckDuckGoApiKey = "api_key_duck_duck_go",
            fastMailApiKey = "api_key_fast_mail",
            anonAddyApiAccessToken = "access_token_anon_addy",
            anonAddyDomainName = "anonaddy.com",
            forwardEmailApiAccessToken = "access_token_forward_email",
            forwardEmailDomainName = "forwardemail.net",
            emailWebsite = "email.example.com",
        )

        val key = "bwPreferencesStorage:usernameGenerationOptions_$userId"
        fakeSharedPreferences.edit { putString(key, json.encodeToString(options)) }

        val result = generatorDiskSource.getUsernameGenerationOptions(userId)

        assertEquals(options, result)
    }

    @Test
    fun `getUsernameGenerationOptions should return null when options are not available`() {
        val userId = "user123"

        val result = generatorDiskSource.getUsernameGenerationOptions(userId)

        assertNull(result)
    }

    @Test
    fun `storeUsernameGenerationOptions should correctly store options`() {
        val userId = "user123"
        val options = UsernameGenerationOptions(
            type = UsernameGenerationOptions.UsernameType.RANDOM_WORD,
            serviceType = UsernameGenerationOptions.ForwardedEmailServiceType.NONE,
            capitalizeRandomWordUsername = true,
            includeNumberRandomWordUsername = false,
            plusAddressedEmail = "example+plus@gmail.com",
            catchAllEmailDomain = "example.com",
            firefoxRelayApiAccessToken = "access_token_firefox_relay",
            simpleLoginApiKey = "api_key_simple_login",
            duckDuckGoApiKey = "api_key_duck_duck_go",
            fastMailApiKey = "api_key_fast_mail",
            anonAddyApiAccessToken = "access_token_anon_addy",
            anonAddyDomainName = "anonaddy.com",
            forwardEmailApiAccessToken = "access_token_forward_email",
            forwardEmailDomainName = "forwardemail.net",
            emailWebsite = "email.example.com",
        )

        val key = "bwPreferencesStorage:usernameGenerationOptions_$userId"

        generatorDiskSource.storeUsernameGenerationOptions(userId, options)

        val storedValue = fakeSharedPreferences.getString(key, null)
        assertNotNull(storedValue)
        assertEquals(json.encodeToString(options), storedValue)
    }
}
