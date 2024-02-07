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
    }
}
