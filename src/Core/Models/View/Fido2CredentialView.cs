using System.Text.Json.Serialization;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;

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
        public string UserDisplayName { get; set; }
        public string Counter { get; set; }
        public DateTime CreationDate { get; set; }

        [JsonIgnore]
        public int CounterValue {
            get => int.TryParse(Counter, out var counter) ? counter : 0;
            set => Counter = value.ToString();
        }

        [JsonIgnore]
        public byte[] UserHandleValue {
            get => UserHandle == null ? null : CoreHelpers.Base64UrlDecode(UserHandle);
            set => UserHandle = value == null ? null : CoreHelpers.Base64UrlEncode(value);
        }

        [JsonIgnore]
        public byte[] KeyBytes {
            get => KeyValue == null ? null : CoreHelpers.Base64UrlDecode(KeyValue);
            set => KeyValue = value == null ? null : CoreHelpers.Base64UrlEncode(value);
        }

        [JsonIgnore]
        public bool DiscoverableValue {
            get => bool.TryParse(Discoverable, out var discoverable) && discoverable;
            set => Discoverable = value.ToString().ToLower();
        }

        [JsonIgnore]
        public override string SubTitle => UserName;
        
        public override List<KeyValuePair<string, LinkedIdType>> LinkedFieldOptions => new List<KeyValuePair<string, LinkedIdType>>();
        
        [JsonIgnore]
        public bool CanLaunch => !string.IsNullOrEmpty(RpId);
        [JsonIgnore]
        public string LaunchUri => $"https://{RpId}";

        public bool IsUniqueAgainst(Fido2CredentialView fido2View) => fido2View?.RpId != RpId || fido2View?.UserName != UserName;
    }
}
