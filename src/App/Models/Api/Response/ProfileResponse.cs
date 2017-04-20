using System.Collections.Generic;

namespace Bit.App.Models.Api
{
    public class ProfileResponse
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Email { get; set; }
        public string MasterPasswordHint { get; set; }
        public string Culture { get; set; }
        public bool TwoFactorEnabled { get; set; }
        public IEnumerable<ProfileOrganizationResponseModel> Organizations { get; set; }
    }
}
