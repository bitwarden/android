namespace Bit.Core.Utilities.Fido2
{
    public class Fido2AuthenticatorGetAssertionParams
    {
        public string RpId { get; set; }

        public string CredentialId { get; set; }

        public string Counter { get; set; }
    }
}

