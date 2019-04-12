using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class IdentityData : Data
    {
        public IdentityData() { }

        public IdentityData(IdentityApi data)
        {
            Title = data.Title;
            FirstName = data.FirstName;
            MiddleName = data.MiddleName;
            LastName = data.LastName;
            Address1 = data.Address1;
            Address2 = data.Address2;
            Address3 = data.Address3;
            City = data.City;
            State = data.State;
            PostalCode = data.PostalCode;
            Country = data.Country;
            Company = data.Company;
            Email = data.Email;
            Phone = data.Phone;
            SSN = data.SSN;
            Username = data.Username;
            PassportNumber = data.PassportNumber;
            LicenseNumber = data.LicenseNumber;
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
