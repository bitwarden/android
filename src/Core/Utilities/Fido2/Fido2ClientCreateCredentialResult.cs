using Bit.Core.Utilities.Fido2.Extensions;

namespace Bit.Core.Utilities.Fido2
{
    /// <summary>
    /// The result of creating a new credential.
    /// 
    /// This class is an extended version of the WebAuthn struct:
    /// https://www.w3.org/TR/webauthn-3/#credentialcreationdata-attestationobjectresult
    /// </summary>
    public class Fido2ClientCreateCredentialResult
    {
        public byte[] CredentialId { get; set; }
        public byte[] ClientDataJSON { get; set; }
        public byte[] AttestationObject { get; set; }
        public byte[] AuthData { get; set; }
        public byte[] PublicKey { get; set; }
        public int PublicKeyAlgorithm { get; set; }
        public string[] Transports { get; set; }
        public Fido2CreateCredentialExtensionsResult Extensions { get; set; }
    }
}
