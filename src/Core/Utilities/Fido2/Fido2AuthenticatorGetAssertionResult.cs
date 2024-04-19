using Bit.Core.Models.View;

namespace Bit.Core.Utilities.Fido2
{
    public class Fido2AuthenticatorGetAssertionResult
    {
        public byte[] AuthenticatorData { get; set; }

        public byte[] Signature { get; set; }

        public Fido2SelectedCredential SelectedCredential { get; set; }
    }
}

