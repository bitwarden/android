using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public class Identity : Domain
    {
        private HashSet<string> _map = new HashSet<string>
        {
            "Title",
            "FirstName",
            "MiddleName",
            "LastName",
            "Address1",
            "Address2",
            "Address3",
            "City",
            "State",
            "PostalCode",
            "Country",
            "Company",
            "Email",
            "Phone",
            "SSN",
            "Username",
            "PassportNumber",
            "LicenseNumber"
        };

        public Identity() { }

        public Identity(IdentityData obj, bool alreadyEncrypted = false)
        {
            BuildDomainModel(this, obj, _map, alreadyEncrypted);
        }

        public EncString Title { get; set; }
        public EncString FirstName { get; set; }
        public EncString MiddleName { get; set; }
        public EncString LastName { get; set; }
        public EncString Address1 { get; set; }
        public EncString Address2 { get; set; }
        public EncString Address3 { get; set; }
        public EncString City { get; set; }
        public EncString State { get; set; }
        public EncString PostalCode { get; set; }
        public EncString Country { get; set; }
        public EncString Company { get; set; }
        public EncString Email { get; set; }
        public EncString Phone { get; set; }
        public EncString SSN { get; set; }
        public EncString Username { get; set; }
        public EncString PassportNumber { get; set; }
        public EncString LicenseNumber { get; set; }

        public Task<IdentityView> DecryptAsync(string orgId)
        {
            return DecryptObjAsync(new IdentityView(this), this, _map, orgId);
        }

        public IdentityData ToIdentityData()
        {
            var i = new IdentityData();
            BuildDataModel(this, i, _map);
            return i;
        }
    }
}
