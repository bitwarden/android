namespace Bit.Core.Utilities.Fido2
{
    /// <summary>
    /// This class represents an authenticator's response to a client's request for generation of a 
    /// new authentication assertion given the WebAuthn Relying Party's challenge.
    /// This response contains a cryptographic signature proving possession of the credential private key,
    /// and optionally evidence of user consent to a specific transaction.
    /// 
    /// See: https://www.w3.org/TR/webauthn-2/#iface-authenticatorassertionresponse
    /// </summary>
    public class Fido2ClientAuthenticatorAssertionResponse
    {
        /// <summary>
        /// The JSON-compatible serialization of client data passed to the authenticator by the client
        /// in order to generate this assertion. The exact JSON serialization MUST be preserved, as the
        /// hash of the serialized client data has been computed over it.
        /// </summary>
        public required byte[] ClientDataJSON { get; set; }

        /// <summary>
        /// The authenticator data returned by the authenticator.
        /// </summary>
        public required byte[] AuthenticatorData { get; set; }

        /// <summary>
        /// Raw signature returned from the authenticator.
        /// </summary>
        public required byte[] Signature { get; set; }

        /// <summary>
        /// The user handle returned from the authenticator, or null if the authenticator did not return a user handle. 
        /// </summary>
        public byte[] UserHandle { get; set; } = null;
    }
}
