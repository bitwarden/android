namespace Bit.Core.Models.Api
{
    public class Fido2KeyApi
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
    }
}
