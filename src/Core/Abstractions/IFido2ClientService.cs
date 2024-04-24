using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Abstractions
{
    /// <summary>
    /// This class represents an abstraction of the WebAuthn Client as described by W3C:
    /// https://www.w3.org/TR/webauthn-3/#webauthn-client
    ///
    /// The WebAuthn Client is an intermediary entity typically implemented in the user agent
    /// (in whole, or in part). Conceptually, it underlies the Web Authentication API and embodies
    /// the implementation of the Web Authentication API's operations.
    ///
    /// It is responsible for both marshalling the inputs for the underlying authenticator operations,
    /// and for returning the results of the latter operations to the Web Authentication API's callers.
    /// </summary>
    public interface IFido2ClientService
    {
        /// <summary>
        /// Allows WebAuthn Relying Party scripts to request the creation of a new public key credential source.
        /// For more information please see: https://www.w3.org/TR/webauthn-3/#sctn-createCredential
        /// </summary>
        /// <param name="createCredentialParams">The parameters for the credential creation operation</param>
        /// <param name="extraParams">Extra parameters for the credential creation operation</param>
        /// <returns>The new credential</returns>
        Task<Fido2ClientCreateCredentialResult> CreateCredentialAsync(Fido2ClientCreateCredentialParams createCredentialParams, Fido2ExtraCreateCredentialParams extraParams);

        /// <summary>
        /// Allows WebAuthn Relying Party scripts to discover and use an existing public key credential, with the user’s consent.
        /// Relying Party script can optionally specify some criteria to indicate what credential sources are acceptable to it.
        /// For more information please see: https://www.w3.org/TR/webauthn-3/#sctn-getAssertion
        /// </summary>
        /// <param name="assertCredentialParams">The parameters for the credential assertion operation</param>
        /// <param name="extraParams">Extra parameters for the credential assertion operation</param>
        /// <returns>The asserted credential</returns>
        Task<Fido2ClientAssertCredentialResult> AssertCredentialAsync(Fido2ClientAssertCredentialParams assertCredentialParams, Fido2ExtraAssertCredentialParams extraParams);
    }
}
