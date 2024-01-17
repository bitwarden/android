namespace Bit.Core.Utilities.Fido2
{
    public class Fido2AuthenticatorGetAssertionParams
    {
        /** The caller’s RP ID, as determined by the user agent and the client. */
        public string RpId { get; set; }

        /** The hash of the serialized client data, provided by the client. */
        public byte[] Hash {get; set;}

        public PublicKeyCredentialDescriptor[] AllowCredentialDescriptorList {get; set;}

        /** The effective user verification requirement for assertion, a Boolean value provided by the client. */
        public bool RequireUserVerification {get; set;}
        
        /** CTAP2 authenticators support setting this to false, but we only support the WebAuthn authenticator model which does not have that option. */
        // public bool RequireUserPresence {get; set;} // Always required

        public object Extensions {get; set;}
    }
}

