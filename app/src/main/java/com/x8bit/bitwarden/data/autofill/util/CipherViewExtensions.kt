package com.x8bit.bitwarden.data.autofill.util

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.platform.util.subtitle

/**
 * Creates a single-item [AutofillCipherProvider] based on the given [CipherView].
 */
fun CipherView.toAutofillCipherProvider(): AutofillCipherProvider =
    object : AutofillCipherProvider {
        override suspend fun isVaultLocked(): Boolean = false

        override suspend fun getCardAutofillCiphers(): List<AutofillCipher.Card> {
            val card = this@toAutofillCipherProvider.card ?: return emptyList()
            return listOf(
                AutofillCipher.Card(
                    cipherId = id,
                    name = name,
                    subtitle = subtitle.orEmpty(),
                    cardholderName = card.cardholderName.orEmpty(),
                    code = card.code.orEmpty(),
                    expirationMonth = card.expMonth.orEmpty(),
                    expirationYear = card.expYear.orEmpty(),
                    number = card.number.orEmpty(),
                ),
            )
        }

        override suspend fun getLoginAutofillCiphers(
            uri: String,
        ): List<AutofillCipher.Login> {
            val login = this@toAutofillCipherProvider.login ?: return emptyList()
            return listOf(
                AutofillCipher.Login(
                    cipherId = id,
                    isTotpEnabled = login.totp != null,
                    name = name,
                    password = login.password.orEmpty(),
                    subtitle = subtitle.orEmpty(),
                    username = login.username.orEmpty(),
                ),
            )
        }
    }

/**
 * Returns true when the cipher is not deleted and contains at least one FIDO 2 credential.
 */
val CipherView.isActiveWithFido2Credentials: Boolean
    get() = deletedDate == null && !(login?.fido2Credentials.isNullOrEmpty())
