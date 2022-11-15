using System;
using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.Data;

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
        public DateTime? ResponseDate { get; set; }
        public bool? RequestApproved { get; set; }
        public string Origin { get; set; }
        public string RequestAccessCode { get; set; }
        public Tuple<byte[], byte[]> RequestKeyPair { get; set; }

        public bool IsAnswered => RequestApproved != null && ResponseDate != null;

        public bool IsExpired => CreationDate.ToUniversalTime().AddMinutes(Constants.PasswordlessNotificationTimeoutInMinutes) < DateTime.UtcNow;
    }

    public class PasswordlessLoginsResponse
    {
        public List<PasswordlessLoginResponse> Data { get; set; }
    }
}
