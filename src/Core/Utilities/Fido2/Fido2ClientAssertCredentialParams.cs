namespace Bit.Core.Utilities.Fido2
{
    #nullable enable

    /// <summary>
    /// Parameters for asserting a credential.
    /// 
    /// This class is an extended version of the WebAuthn struct:
    /// https://www.w3.org/TR/webauthn-2/#dictdef-publickeycredentialrequestoptions
    /// </summary>
    public class Fido2ClientAssertCredentialParams
    {
        /// <summary>
        /// A value which is true if and only if the caller’s environment settings object is same-origin with its ancestors.
        /// It is false if caller is cross-origin.
        /// </summary>
        public bool SameOriginWithAncestors { get; set; }

        /// <summary>
        /// The challenge that the selected authenticator signs, along with other data, when producing an authentication
        /// assertion. 
        /// </summary>
        public required byte[] Challenge { get; set; }

        /// <summary>
        /// The relying party identifier claimed by the caller. If omitted, its value will be the CredentialsContainer 
        /// object's relevant settings object's origin's effective domain.
        /// </summary>
        public string RpId { get; set; }

        /// <summary>
        /// The Relying Party's origin (e.g., "https://example.com").
        /// </summary>
        public string Origin { get; set; }
        
        /// <summary>
        /// A list of PublicKeyCredentialDescriptor objects representing public key credentials acceptable to the caller,
        /// in descending order of the caller’s preference (the first item in the list is the most preferred credential,
        /// and so on down the list).
        /// </summary>
        public PublicKeyCredentialDescriptor[] AllowCredentials { get; set; } = [];

        /// <summary>
        /// The Relying Party's requirements regarding user verification for the get() operation.
        /// </summary>
        public string UserVerification { get; set; } = "preferred";

        /// <summary>
        /// This time, in milliseconds, that the caller is willing to wait for the call to complete.
        /// This is treated as a hint, and MAY be overridden by the client.
        /// </summary>
        /// <remarks>
        /// This is not currently supported.
        /// </remarks>
        public int? Timeout { get; set; }
    }
}
