using Bit.Core.Models.Data;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Domain
{
    public class Fido2Credential : Domain
    {
        public static HashSet<string> EncryptablePropertiesToMap => new HashSet<string>
        {
            nameof(CredentialId),
            nameof(Discoverable),
            nameof(KeyType),
            nameof(KeyAlgorithm),
            nameof(KeyCurve),
            nameof(KeyValue),
            nameof(RpId),
            nameof(RpName),
            nameof(UserHandle),
            nameof(UserName),
            nameof(UserDisplayName),
            nameof(Counter)
        };

        public static HashSet<string> NonEncryptablePropertiesToMap => new HashSet<string>
        {
            nameof(CreationDate)
        };

        public static HashSet<string> AllPropertiesToMap => new HashSet<string>(EncryptablePropertiesToMap.Concat(NonEncryptablePropertiesToMap));

        public Fido2Credential() { }

        public Fido2Credential(Fido2CredentialData data, bool alreadyEncrypted = false)
        {
            BuildDomainModel(this, data, AllPropertiesToMap, alreadyEncrypted, NonEncryptablePropertiesToMap);
        }

        public EncString CredentialId { get; set; }
        public EncString Discoverable { get; set; }
        public EncString KeyType { get; set; }
        public EncString KeyAlgorithm { get; set; }
        public EncString KeyCurve { get; set; }
        public EncString KeyValue { get; set; }
        public EncString RpId { get; set; }
        public EncString RpName { get; set; }
        public EncString UserHandle { get; set; }
        public EncString UserName { get; set; }
        public EncString UserDisplayName { get; set; }
        public EncString Counter { get; set; }
        public DateTime CreationDate { get; set; }

        public async Task<Fido2CredentialView> DecryptAsync(string orgId, SymmetricCryptoKey key = null)
        {
            return await DecryptObjAsync(new Fido2CredentialView(this), this, EncryptablePropertiesToMap, orgId, key);
        }

        public Fido2CredentialData ToFido2CredentialData()
        {
            var data = new Fido2CredentialData();
            BuildDataModel(this, data, AllPropertiesToMap, NonEncryptablePropertiesToMap);
            return data;
        }
    }
}
