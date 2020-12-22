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
            AccessBusinessPortal = obj.AccessBusinessPortal;
            AccessEventLogs = obj.AccessEventLogs;
            AccessImportExport = obj.AccessImportExport;
            AccessReports = obj.AccessReports;
            ManageAllCollections = obj.ManageAllCollections;
            ManageAssignedCollections = obj.ManageAssignedCollections;
            ManageGroups = obj.ManageGroups;
            ManagePolicies = obj.ManagePolicies;
            ManageUsers = obj.ManageUsers;
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
        public int Seats { get; set; }
        public int MaxCollections { get; set; }
        public short? MaxStorageGb { get; set; }
        public bool AccessBusinessPortal { get; set; }
        public bool AccessEventLogs { get; set; }
        public bool AccessImportExport { get; set; }
        public bool AccessReports { get; set; }
        public bool ManageAllCollections { get; set; }
        public bool ManageAssignedCollections { get; set; }
        public bool ManageGroups { get; set; }
        public bool ManagePolicies { get; set; }
        public bool ManageUsers { get; set; }

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
        public bool canAccessBusinessPortl => IsAdmin || AccessBusinessPortal;
        public bool canAccessEventLogs => IsAdmin || AccessEventLogs;
        public bool canAccessImportExport => IsAdmin || canAccessImportExport;
        public bool canAccessReports => IsAdmin || canAccessReports;
        public bool canManageAllCollections => IsAdmin || ManageAllCollections;
        public bool canManageAssignedCollections => IsManager || canManageAssignedCollections;
        public bool canManageGroups => IsAdmin || canManageGroups;
        public bool canManagePolicies => IsAdmin || canManagePolicies;
        public bool canManageUser => IsAdmin || canManageUser;
    }
}
