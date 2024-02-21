namespace Bit.Core.Utilities.Fido2
{
    public class Fido2AuthenticatorGetAssertionResult
    {
        public byte[] AuthenticatorData { get; set; }

        public byte[] Signature { get; set; }

        public Fido2AuthenticatorGetAssertionSelectedCredential SelectedCredential { get; set; }
    }

    public class Fido2AuthenticatorGetAssertionSelectedCredential {
        public byte[] Id { get; set; }

        #nullable enable
        public byte[]? UserHandle { get; set; }
    }
}

