using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Domain
{
    public class Fido2Key : Domain
    {
        public static HashSet<string> EncryptableProperties => new HashSet<string>
        {
            nameof(NonDiscoverableId),
            nameof(KeyType),
            nameof(KeyAlgorithm),
            nameof(KeyCurve),
            nameof(KeyValue),
            nameof(RpId),
            nameof(RpName),
            nameof(UserHandle),
            nameof(UserName),
            nameof(Counter)
        };

        public Fido2Key() { }

        public Fido2Key(Fido2KeyData data, bool alreadyEncrypted = false)
        {
            BuildDomainModel(this, data, EncryptableProperties, alreadyEncrypted);
        }

        public EncString NonDiscoverableId { get; set; }
        public EncString KeyType { get; set; }
        public EncString KeyAlgorithm { get; set; }
        public EncString KeyCurve { get; set; }
        public EncString KeyValue { get; set; }
        public EncString RpId { get; set; }
        public EncString RpName { get; set; }
        public EncString UserHandle { get; set; }
        public EncString UserName { get; set; }
        public EncString Counter { get; set; }

        public async Task<Fido2KeyView> DecryptAsync(string orgId)
        {
            return await DecryptObjAsync(new Fido2KeyView(), this, EncryptableProperties, orgId);
        }

        public Fido2KeyData ToFido2KeyData()
        {
            var data = new Fido2KeyData();
            BuildDataModel(this, data, EncryptableProperties);
            return data;
        }
    }
}
