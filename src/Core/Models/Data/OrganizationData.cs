using Bit.Core.Enums;
using Bit.Core.Models.Response;

namespace Bit.Core.Models.Data
{
    public class OrganizationData : Data
    {
        public OrganizationData() { }

        public OrganizationData(ProfileOrganizationResponse response)
        {
            Id = response.Id;
            Name = response.Name;
            Status = response.Status;
            Type = response.Type;
            Enabled = response.Enabled;
            UseGroups = response.UseGroups;
            UseDirectory = response.UseDirectory;
            UseEvents = response.UseEvents;
            UseTotp = response.UseTotp;
            Use2fa = response.Use2fa;
            UseApi = response.UseApi;
            UsePolicies = response.UsePolicies;
            SelfHost = response.SelfHost;
            UsersGetPremium = response.UsersGetPremium;
            Seats = response.Seats;
            MaxCollections = response.MaxCollections;
            MaxStorageGb = response.MaxStorageGb;
            Permissions = response.Permissions ?? new Permissions();
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public OrganizationUserStatusType Status { get; set; }
        public OrganizationUserType Type { get; set; }
        public bool Enabled { get; set; }
        public bool UseGroups { get; set; }
        public bool UseDirectory { get; set; }
        public bool UseEvents { get; set; }
        public bool UseTotp { get; set; }
        public bool Use2fa { get; set; }
        public bool UseApi { get; set; }
        public bool UsePolicies { get; set; }
        public bool SelfHost { get; set; }
        public bool UsersGetPremium { get; set; }
        public int? Seats { get; set; }
        public short? MaxCollections { get; set; }
        public short? MaxStorageGb { get; set; }
        public Permissions Permissions { get; set; } = new Permissions();
    }
}
