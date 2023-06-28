using System;
namespace Bit.Core.Models.Request
{
    public class UpdateTrustedDeviceKeysRequest
    {
        public string DeviceIdentifier { get; set; }
        public string DevicePublicKeyEncryptedUserKey { get; set; }
        public string UserKeyEncryptedDevicePublicKey { get; set; }
        public string DeviceKeyEncryptedDevicePrivateKey { get; set; }
    }
}

