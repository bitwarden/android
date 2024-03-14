using Bit.Core.Models.View;

namespace Bit.Core.Utilities.Fido2
{
    /// <summary>
    /// The result of asserting a credential.
    /// 
    /// See: https://www.w3.org/TR/webauthn-2/#publickeycredential
    /// </summary>
    public class Fido2ClientAssertCredentialResult
    {
        /// <summary>
        /// Base64url encoding of the credential identifer.
        /// </summary>
        public required string Id { get; set; }

        /// <summary>
        /// The credential identifier.
        /// </summary>
        public required byte[] RawId { get; set; }

        /// <summary>
        /// The JSON-compatible serialization of client datapassed to the authenticator by the client in
        /// order to generate this assertion.
        /// </summary>
        public required byte[] ClientDataJSON { get; set; }

        /// <summary>
        /// The authenticator data returned by the authenticator.
        /// </summary>
        public required byte[] AuthenticatorData { get; set; }

        /// <summary>
        /// The raw signature returned from the authenticator.
        /// </summary>
        public required byte[] Signature { get; set; }

        /// <summary>
        /// The user handle returned from the authenticator, or null if the authenticator did not
        /// return a user handle.
        /// </summary>
        public byte[]? UserHandle { get; set; }

        /// <summary>
        /// The selected cipher login item that has the credential
        /// </summary>
        public CipherView? Cipher { get; set; }
    }
}
