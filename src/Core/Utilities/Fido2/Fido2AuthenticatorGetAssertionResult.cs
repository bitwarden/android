using System;
namespace Bit.Core.Utilities.Fido2
{
    public class Fido2AuthenticatorGetAssertionResult
    {
        public byte[] AuthenticatorData { get; set; }

        public byte[] Signature { get; set; }
    }
}

