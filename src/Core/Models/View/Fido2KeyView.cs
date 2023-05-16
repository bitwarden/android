using System;
using System.Collections.Generic;
using Bit.Core.Enums;

namespace Bit.Core.Models.View
{
    public class Fido2KeyView : ItemView, ILaunchableView
    {
        public string NonDiscoverableId { get; set; }
        public string KeyType { get; set; } = Constants.DefaultFido2KeyType;
        public string KeyAlgorithm { get; set; } = Constants.DefaultFido2KeyAlgorithm;
        public string KeyCurve { get; set; } = Constants.DefaultFido2KeyCurve;
        public string KeyValue { get; set; }
        public string RpId { get; set; }
        public string RpName { get; set; }
        public string UserHandle { get; set; }
        public string UserName { get; set; }
        public string Counter { get; set; }

        public override string SubTitle => UserName;
        public override List<KeyValuePair<string, LinkedIdType>> LinkedFieldOptions => new List<KeyValuePair<string, LinkedIdType>>();
        public bool CanLaunch => !string.IsNullOrEmpty(RpId);
        public string LaunchUri => $"https://{RpId}";
    }
}
