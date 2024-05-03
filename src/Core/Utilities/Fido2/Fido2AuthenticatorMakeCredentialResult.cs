
namespace Bit.Core.Utilities.Fido2
{
    public class Fido2AuthenticatorMakeCredentialResult
    {
        public byte[] CredentialId { get; set; }

        public byte[] AttestationObject { get; set; }

        public byte[] AuthData { get; set; }

        public byte[] PublicKey { get; set; }

        public int PublicKeyAlgorithm { get; set; }
    }
}
