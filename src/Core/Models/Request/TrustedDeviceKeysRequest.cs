
namespace Bit.Core.Models.Request
{
    public class TrustedDeviceKeysRequest
    {
        public string EncryptedUserKey { get; set; }
        public string EncryptedPublicKey { get; set; }
        public string EncryptedPrivateKey { get; set; }
    }
}
