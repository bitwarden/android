using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class ProfileResponse
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Email { get; set; }
        public bool EmailVerified { get; set; }
        public bool Premium { get; set; }
        public string MasterPasswordHint { get; set; }
        public string Culture { get; set; }
        public bool TwoFactorEnabled { get; set; }
        public string Key { get; set; }
        public string PrivateKey { get; set; }
        public string SecurityStamp { get; set; }
        public List<ProfileOrganizationResponse> Organizations { get; set; }
    }
}
