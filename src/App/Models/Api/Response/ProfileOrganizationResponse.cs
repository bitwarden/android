using Bit.App.Enums;

namespace Bit.App.Models.Api
{
    public class ProfileOrganizationResponseModel
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Key { get; set; }
        public OrganizationUserStatusType Status { get; set; }
        public OrganizationUserType Type { get; set; }
        public bool Enabled { get; set; }
    }
}
