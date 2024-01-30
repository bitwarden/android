namespace Bit.Core.Utilities.Fido2
{
    public class Fido2AuthenticatorGetAssertionParams
    {
        /** The caller’s RP ID, as determined by the user agent and the client. */
        public string RpId { get; set; }

        /** The hash of the serialized client data, provided by the client. */
        public byte[] ClientDataHash { get; set; }

        public PublicKeyCredentialDescriptor[] AllowCredentialDescriptorList { get; set; }

        /// <summary>
        /// Instructs the authenticator to require a user-verifying gesture in order to complete the request. Examples of such gestures are fingerprint scan or a PIN.
        /// </summary>
        public bool RequireUserVerification { get; set; }
        
        /// <summary>
        /// Instructs the authenticator to require user consent to complete the operation.
        /// </summary>
        public bool RequireUserPresence { get; set; }

        public object Extensions { get; set; }
    }
}

