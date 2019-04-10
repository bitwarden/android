using Bit.Core.Enums;
using System;
using System.Collections.Generic;
using System.Text;

namespace Bit.Core.Models.Request
{
    public class TokenRequest
    {
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public string Token { get; set; }
        public TwoFactorProviderType Provider { get; set; }
        public bool Remember { get; set; }
        public DeviceRequest Device { get; set; }
    }
}
