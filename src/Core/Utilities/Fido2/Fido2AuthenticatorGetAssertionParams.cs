namespace Bit.Core.Utilities.Fido2
{
    public class Fido2AuthenticatorGetAssertionParams
    {
        /** The caller’s RP ID, as determined by the user agent and the client. */
        public string RpId { get; set; }

        /** The hash of the serialized client data, provided by the client. */
        public byte[] Hash { get; set; }

        public PublicKeyCredentialDescriptor[] AllowCredentialDescriptorList { get; set; }

        /// <summary>
        /// Instructs the authenticator the user verification preference in order to complete the request. Examples of UV gestures are fingerprint scan or a PIN.
        /// </summary>
        public Fido2UserVerificationPreference UserVerificationPreference { get; set; }

        /// <summary>
        /// The challenge to be signed by the authenticator.
        /// </summary>
        public byte[] Challenge { get; set; }

        public object Extensions { get; set; }
    }
}

