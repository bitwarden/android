package com.x8bit.bitwarden.data.autofill.util

import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.platform.util.identityAutofillAddress
import com.x8bit.bitwarden.data.platform.util.identityAutofillName
import com.x8bit.bitwarden.data.platform.util.isActive
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
                    brand = card.brand.orEmpty(),
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
                    website = uri,
                ),
            )
        }

        override suspend fun getIdentityAutofillCiphers(): List<AutofillCipher.Identity> {
            val identityView = this@toAutofillCipherProvider.identity ?: return emptyList()
            return listOf(
                AutofillCipher.Identity(
                    cipherId = id,
                    name = name,
                    subtitle = subtitle.orEmpty(),
                    fullName = identityView.identityAutofillName.orEmpty(),
                    fullAddress = identityView.identityAutofillAddress.orEmpty(),
                    title = identityView.title.orEmpty(),
                    firstName = identityView.firstName.orEmpty(),
                    middleName = identityView.middleName.orEmpty(),
                    lastName = identityView.lastName.orEmpty(),
                    address1 = identityView.address1.orEmpty(),
                    address2 = identityView.address2.orEmpty(),
                    address3 = identityView.address3.orEmpty(),
                    city = identityView.city.orEmpty(),
                    state = identityView.state.orEmpty(),
                    postalCode = identityView.postalCode.orEmpty(),
                    country = identityView.country.orEmpty(),
                    company = identityView.company.orEmpty(),
                    email = identityView.email.orEmpty(),
                    phone = identityView.phone.orEmpty(),
                    ssn = identityView.ssn.orEmpty(),
                    passportNumber = identityView.passportNumber.orEmpty(),
                    licenseNumber = identityView.licenseNumber.orEmpty(),
                ),
            )
        }
    }

/**
 * Returns true when the cipher is not archived, not deleted and contains at least one FIDO 2
 * credential.
 */
val CipherView.isActiveWithFido2Credentials: Boolean
    get() = isActive && !(login?.fido2Credentials.isNullOrEmpty())

/**
 * Returns true when the cipher is not archived, not deleted and contains at least one Password
 * credential.
 */
val CipherView.isActiveWithPasswordCredentials: Boolean
    get() = isActive && !(login?.password.isNullOrEmpty())
