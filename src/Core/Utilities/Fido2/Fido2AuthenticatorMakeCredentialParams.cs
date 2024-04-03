namespace Bit.Core.Utilities.Fido2
{
    public class Fido2AuthenticatorMakeCredentialParams
    {
        /// <summary>
        /// The Relying Party's PublicKeyCredentialRpEntity.
        /// </summary>
        public PublicKeyCredentialRpEntity RpEntity { get; set; }

        /// <summary>
        /// The Relying Party's PublicKeyCredentialRpEntity.
        /// </summary>
        public PublicKeyCredentialUserEntity UserEntity { get; set; }

        /// <summary>
        /// The hash of the serialized client data, provided by the client.
        /// </summary>
        public byte[] Hash { get; set; }

        /// <summary>
        /// A sequence of pairs of PublicKeyCredentialType and public key algorithms (COSEAlgorithmIdentifier) requested by the Relying Party. This sequence is ordered from most preferred to least preferred. The authenticator makes a best-effort to create the most preferred credential that it can.
        /// </summary>
        public PublicKeyCredentialParameters[] CredTypesAndPubKeyAlgs { get; set; }

        /// <summary>
        ///An OPTIONAL list of PublicKeyCredentialDescriptor objects provided by the Relying Party with the intention that, if any of these are known to the authenticator, it SHOULD NOT create a new credential. excludeCredentialDescriptorList contains a list of known credentials.
        /// </summary>
        public PublicKeyCredentialDescriptor[] ExcludeCredentialDescriptorList { get; set; }

        /// <summary>
        /// The effective resident key requirement for credential creation, a Boolean value determined by the client. Resident is synonymous with discoverable. */
        /// </summary>
        public bool RequireResidentKey { get; set; }

        /// <summary>
        /// The effective user verification preference for assertion, provided by the client.
        /// </summary>
        public Fido2UserVerificationPreference UserVerificationPreference { get; set; }

        /// <summary>
        /// CTAP2 authenticators support setting this to false, but we only support the WebAuthn authenticator model which does not have that option.
        /// </summary>
        // public bool RequireUserPresence { get; set; } // Always required

        /// <summary>
        /// The authenticator's attestation preference, a string provided by the client. This is a hint that the client gives to the authenticator about what kind of attestation statement it would like. The authenticator makes a best-effort to satisfy the preference.
        /// Note: Attestation statements are not supported at this time.
        /// </summary>
        // public string AttestationPreference { get; set; }

        public object Extensions { get; set; }
    }
}
