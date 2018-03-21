using Bit.App.Models.Api;
using System;

namespace Bit.App.Models.Data
{
    public class IdentityDataModel : CipherDataModel
    {
        public IdentityDataModel() { }

        public IdentityDataModel(CipherResponse response)
            : base(response)
        {
            if(response?.Identity == null)
            {
                throw new ArgumentNullException(nameof(response.Identity));
            }

            Title = response.Identity.Title;
            FirstName = response.Identity.FirstName;
            MiddleName = response.Identity.MiddleName;
            LastName = response.Identity.LastName;
            Address1 = response.Identity.Address1;
            Address2 = response.Identity.Address2;
            Address3 = response.Identity.Address3;
            City = response.Identity.City;
            State = response.Identity.State;
            PostalCode = response.Identity.PostalCode;
            Country = response.Identity.Country;
            Company = response.Identity.Company;
            Email = response.Identity.Email;
            Phone = response.Identity.Phone;
            SSN = response.Identity.SSN;
            Username = response.Identity.Username;
            PassportNumber = response.Identity.PassportNumber;
            LicenseNumber = response.Identity.LicenseNumber;
        }

        public string Title { get; set; }
        public string FirstName { get; set; }
        public string MiddleName { get; set; }
        public string LastName { get; set; }
        public string Address1 { get; set; }
        public string Address2 { get; set; }
        public string Address3 { get; set; }
        public string City { get; set; }
        public string State { get; set; }
        public string PostalCode { get; set; }
        public string Country { get; set; }
        public string Company { get; set; }
        public string Email { get; set; }
        public string Phone { get; set; }
        public string SSN { get; set; }
        public string Username { get; set; }
        public string PassportNumber { get; set; }
        public string LicenseNumber { get; set; }
    }
}
