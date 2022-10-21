using System;
using Bit.Core.Enums;

namespace Bit.Core.Models.Response
{
    public class PasswordlessLoginResponse
    {
        public string Id { get; set; }
        public string PublicKey { get; set; }
        public string RequestDeviceType { get; set; }
        public string RequestIpAddress { get; set; }
        public string RequestFingerprint { get; set; }
        public string Key { get; set; }
        public string MasterPasswordHash { get; set; }
        public DateTime CreationDate { get; set; }
        public bool RequestApproved { get; set; }
        public string Origin { get; set; }
    }
}
