using System;
namespace Bit.Core.Models.Domain
{
    public class PendingAdminAuthRequest
    {
        public string Id { get; set; }
        public byte[] PrivateKey { get; set; }
    }
}

