using Bit.Core.Models.Domain;

namespace Bit.Core.Models.Api
{
    public class Fido2CredentialApi
    {
        public Fido2CredentialApi()
        {
        }

        public Fido2CredentialApi(Fido2Credential fido2Key)
        {
            CredentialId = fido2Key.CredentialId?.EncryptedString;
            Discoverable = fido2Key.Discoverable?.EncryptedString;
            KeyType = fido2Key.KeyType?.EncryptedString;
            KeyAlgorithm = fido2Key.KeyAlgorithm?.EncryptedString;
            KeyCurve = fido2Key.KeyCurve?.EncryptedString;
            KeyValue = fido2Key.KeyValue?.EncryptedString;
            RpId = fido2Key.RpId?.EncryptedString;
            RpName = fido2Key.RpName?.EncryptedString;
            UserHandle = fido2Key.UserHandle?.EncryptedString;
            UserName = fido2Key.UserName?.EncryptedString;
            UserDisplayName = fido2Key.UserDisplayName?.EncryptedString;
            Counter = fido2Key.Counter?.EncryptedString;
            CreationDate = fido2Key.CreationDate;
        }

        public string CredentialId { get; set; }
        public string Discoverable { get; set; }
        public string KeyType { get; set; } = Constants.DefaultFido2CredentialType;
        public string KeyAlgorithm { get; set; } = Constants.DefaultFido2CredentialAlgorithm;
        public string KeyCurve { get; set; } = Constants.DefaultFido2CredentialCurve;
        public string KeyValue { get; set; }
        public string RpId { get; set; }
        public string RpName { get; set; }
        public string UserHandle { get; set; }
        public string UserName { get; set; }
        public string UserDisplayName { get; set; }
        public string Counter { get; set; }
        public DateTime CreationDate { get; set; }
    }
}
