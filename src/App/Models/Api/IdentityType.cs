namespace Bit.App.Models.Api
{
    public class IdentityType
    {
        public IdentityType() { }

        public IdentityType(Cipher cipher)
        {
            Title = cipher.Identity.Title?.EncryptedString;
            FirstName = cipher.Identity.FirstName?.EncryptedString;
            MiddleName = cipher.Identity.MiddleName?.EncryptedString;
            LastName = cipher.Identity.LastName?.EncryptedString;
            Address1 = cipher.Identity.Address1?.EncryptedString;
            Address2 = cipher.Identity.Address2?.EncryptedString;
            Address3 = cipher.Identity.Address3?.EncryptedString;
            City = cipher.Identity.City?.EncryptedString;
            State = cipher.Identity.State?.EncryptedString;
            PostalCode = cipher.Identity.PostalCode?.EncryptedString;
            Country = cipher.Identity.Country?.EncryptedString;
            Company = cipher.Identity.Company?.EncryptedString;
            Email = cipher.Identity.Email?.EncryptedString;
            Phone = cipher.Identity.Phone?.EncryptedString;
            SSN = cipher.Identity.SSN?.EncryptedString;
            Username = cipher.Identity.Username?.EncryptedString;
            PassportNumber = cipher.Identity.PassportNumber?.EncryptedString;
            LicenseNumber = cipher.Identity.LicenseNumber?.EncryptedString;
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
