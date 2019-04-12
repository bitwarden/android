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

        public CipherString Title { get; set; }
        public CipherString FirstName { get; set; }
        public CipherString MiddleName { get; set; }
        public CipherString LastName { get; set; }
        public CipherString Address1 { get; set; }
        public CipherString Address2 { get; set; }
        public CipherString Address3 { get; set; }
        public CipherString City { get; set; }
        public CipherString State { get; set; }
        public CipherString PostalCode { get; set; }
        public CipherString Country { get; set; }
        public CipherString Company { get; set; }
        public CipherString Email { get; set; }
        public CipherString Phone { get; set; }
        public CipherString SSN { get; set; }
        public CipherString Username { get; set; }
        public CipherString PassportNumber { get; set; }
        public CipherString LicenseNumber { get; set; }

        public Task<IdentityView> DecryptAsync(string orgId)
        {
            return DecryptObjAsync(new IdentityView(this), this, _map, orgId);
        }

        public IdentityData ToLoginUriData()
        {
            var i = new IdentityData();
            BuildDataModel(this, i, _map);
            return i;
        }
    }
}
