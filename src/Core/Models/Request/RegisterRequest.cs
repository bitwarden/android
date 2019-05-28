using System;
using Bit.Core.Enums;

namespace Bit.Core.Models.Request
{
    public class RegisterRequest
    {
        public string Name { get; set; }
        public string Email { get; set; }
        public string MasterPasswordHash { get; set; }
        public string MasterPasswordHint { get; set; }
        public string Key { get; set; }
        public KeysRequest Keys { get; set; }
        public string Token { get; set; }
        public Guid? OrganizationUserId { get; set; }
        public KdfType? Kdf { get; set; }
        public int? KdfIterations { get; set; }
    }
}
