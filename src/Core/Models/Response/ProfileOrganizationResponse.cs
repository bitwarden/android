using Bit.Core.Enums;
using Bit.Core.Models.Data;

namespace Bit.Core.Models.Response
{
    public class ProfileOrganizationResponse
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public bool UseGroups { get; set; }
        public bool UseDirectory { get; set; }
        public bool UseEvents { get; set; }
        public bool UseTotp { get; set; }
        public bool Use2fa { get; set; }
        public bool UseApi { get; set; }
        public bool UsePolicies { get; set; }
        public bool UsersGetPremium { get; set; }
        public bool SelfHost { get; set; }
        public int? Seats { get; set; }
        public short? MaxCollections { get; set; }
        public short? MaxStorageGb { get; set; }
        public string Key { get; set; }
        public OrganizationUserStatusType Status { get; set; }
        public OrganizationUserType Type { get; set; }
        public bool Enabled { get; set; }
        public Permissions Permissions { get; set; } = new Permissions();
    }
}
