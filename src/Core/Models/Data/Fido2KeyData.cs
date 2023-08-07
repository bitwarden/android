using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class Fido2KeyData : Data
    {
        public Fido2KeyData() { }

        public Fido2KeyData(Fido2KeyApi apiData)
        {
            NonDiscoverableId = apiData.NonDiscoverableId;
            KeyType = apiData.KeyType;
            KeyAlgorithm = apiData.KeyAlgorithm;
            KeyCurve = apiData.KeyCurve;
            KeyValue = apiData.KeyValue;
            RpId = apiData.RpId;
            RpName = apiData.RpName;
            UserHandle = apiData.UserHandle;
            UserName = apiData.UserName;
            Counter = apiData.Counter;
        }

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
    }
}
