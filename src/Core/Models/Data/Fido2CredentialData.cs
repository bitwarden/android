using System;
using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class Fido2CredentialData : Data
    {
        public Fido2CredentialData() { }

        public Fido2CredentialData(Fido2CredentialApi apiData)
        {
            CredentialId = apiData.CredentialId;
            Discoverable = apiData.Discoverable;
            KeyType = apiData.KeyType;
            KeyAlgorithm = apiData.KeyAlgorithm;
            KeyCurve = apiData.KeyCurve;
            KeyValue = apiData.KeyValue;
            RpId = apiData.RpId;
            RpName = apiData.RpName;
            UserHandle = apiData.UserHandle;
            UserName = apiData.UserName;
            UserDisplayName = apiData.UserDisplayName;
            Counter = apiData.Counter;
            CreationDate = apiData.CreationDate;
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
