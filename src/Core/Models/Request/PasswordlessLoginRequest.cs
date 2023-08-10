using System;

namespace Bit.Core.Models.Request
{
    public class PasswordlessLoginRequest
    {
        public PasswordlessLoginRequest(string key, string masterPasswordHash, string deviceIdentifier,
            bool requestApproved)
        {
            Key = key ?? throw new ArgumentNullException(nameof(key));
            MasterPasswordHash = masterPasswordHash;
            DeviceIdentifier = deviceIdentifier ?? throw new ArgumentNullException(nameof(deviceIdentifier));
            RequestApproved = requestApproved;
        }

        public string Key { get; set; }
        public string MasterPasswordHash { get; set; }
        public string DeviceIdentifier { get; set; }
        public bool RequestApproved { get; set; }
    }
}
