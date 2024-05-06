using Newtonsoft.Json;

namespace Bit.Core.Utilities.DigitalAssetLinks
{
    public class Target
    {
        public string Namespace { get; set; }
        [JsonProperty("package_name")]
        public string PackageName { get; set; }
        [JsonProperty("sha256_cert_fingerprints")]
        public IEnumerable<string> Sha256CertFingerprints { get; set; }

    }
}
