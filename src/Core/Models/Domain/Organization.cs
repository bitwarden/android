using Bit.Core.Enums;
using Bit.Core.Models.Data;

namespace Bit.Core.Models.Domain
{
    public class Organization
    {
        public Organization() { }

        public Organization(OrganizationData obj)
        {
            Id = obj.Id;
            Name = obj.Name;
            Status = obj.Status;
            Type = obj.Type;
            Enabled = obj.Enabled;
            UseGroups = obj.UseGroups;
            UseDirectory = obj.UseDirectory;
            UseEvents = obj.UseEvents;
            UseTotp = obj.UseTotp;
            Use2fa = obj.Use2fa;
            UseApi = obj.UseApi;
            UsePolicies = obj.UsePolicies;
            SelfHost = obj.SelfHost;
            UsersGetPremium = obj.UsersGetPremium;
            Seats = obj.Seats;
            MaxCollections = obj.MaxCollections;
            MaxStorageGb = obj.MaxStorageGb;
            Permissions = obj.Permissions ?? new Permissions();
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

        public bool CanAccess
        {
            get
            {
                if (Type == OrganizationUserType.Owner)
                {
                    return true;
                }
                return Enabled && Status == OrganizationUserStatusType.Confirmed;
            }
        }

        public bool IsManager
        {
            get
            {
                switch (Type)
                {
                    case OrganizationUserType.Owner:
                    case OrganizationUserType.Admin:
                    case OrganizationUserType.Manager:
                        return true;
                    default:
                        return false;
                }
            }
        }

        public bool IsAdmin => Type == OrganizationUserType.Owner || Type == OrganizationUserType.Admin;
        public bool IsOwner => Type == OrganizationUserType.Owner;
        public bool IsCustom => Type == OrganizationUserType.Custom;
        public bool canAccessBusinessPortl => IsAdmin || Permissions.AccessBusinessPortal;
        public bool canAccessEventLogs => IsAdmin || Permissions.AccessEventLogs;
        public bool canAccessImportExport => IsAdmin || Permissions.AccessImportExport;
        public bool canAccessReports => IsAdmin || Permissions.AccessReports;
        public bool canManageAllCollections => IsAdmin || Permissions.ManageAllCollections;
        public bool canManageAssignedCollections => IsManager || Permissions.ManageAssignedCollections;
        public bool canManageGroups => IsAdmin || Permissions.ManageGroups;
        public bool canManagePolicies => IsAdmin || Permissions.ManagePolicies;
        public bool canManageUser => IsAdmin || Permissions.ManageUsers;
    }
}
