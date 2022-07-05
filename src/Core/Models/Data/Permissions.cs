namespace Bit.Core.Models.Data
{
    public class Permissions
    {
        public bool AccessBusinessPortal { get; set; }
        public bool AccessEventLogs { get; set; }
        public bool AccessImportExport { get; set; }
        public bool AccessReports { get; set; }
        public bool EditAssignedCollections { get; set; }
        public bool DeleteAssignedCollections { get; set; }
        public bool CreateNewCollections { get; set; }
        public bool EditAnyCollection { get; set; }
        public bool DeleteAnyCollection { get; set; }
        public bool ManageGroups { get; set; }
        public bool ManagePolicies { get; set; }
        public bool ManageSso { get; set; }
        public bool ManageUsers { get; set; }
    }
}
