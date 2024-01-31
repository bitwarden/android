using System;
using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class Fido2CredentialView : ItemView, ILaunchableView
    {
        public Fido2CredentialView()
        {
        }

        public Fido2CredentialView(Fido2Credential fido2Credential)
        {
            CreationDate = fido2Credential.CreationDate;
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
        public string Counter { get; set; }
        public DateTime CreationDate { get; set; }

        public override string SubTitle => UserName;
        public override List<KeyValuePair<string, LinkedIdType>> LinkedFieldOptions => new List<KeyValuePair<string, LinkedIdType>>();
        public bool IsDiscoverable => !string.IsNullOrWhiteSpace(Discoverable);
        public bool CanLaunch => !string.IsNullOrEmpty(RpId);
        public string LaunchUri => $"https://{RpId}";

        public bool IsUniqueAgainst(Fido2CredentialView fido2View) => fido2View?.RpId != RpId || fido2View?.UserName != UserName;
    }
}
