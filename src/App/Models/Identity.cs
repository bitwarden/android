using Bit.App.Models.Data;
using Newtonsoft.Json;
using System;

namespace Bit.App.Models
{
    public class Identity
    {
        public Identity() { }

        public Identity(CipherData data)
        {
            IdentityDataModel deserializedData;
            if(data.Card != null)
            {
                deserializedData = JsonConvert.DeserializeObject<IdentityDataModel>(data.Card);
            }
            else if(data.Data != null)
            {
                deserializedData = JsonConvert.DeserializeObject<IdentityDataModel>(data.Data);
            }
            else
            {
                throw new ArgumentNullException(nameof(data.Identity));
            }

            Title = deserializedData.Title != null ? new CipherString(deserializedData.Title) : null;
            FirstName = deserializedData.FirstName != null ? new CipherString(deserializedData.FirstName) : null;
            MiddleName = deserializedData.MiddleName != null ? new CipherString(deserializedData.MiddleName) : null;
            LastName = deserializedData.LastName != null ? new CipherString(deserializedData.LastName) : null;
            Address1 = deserializedData.Address1 != null ? new CipherString(deserializedData.Address1) : null;
            Address2 = deserializedData.Address2 != null ? new CipherString(deserializedData.Address2) : null;
            Address3 = deserializedData.Address3 != null ? new CipherString(deserializedData.Address3) : null;
            City = deserializedData.City != null ? new CipherString(deserializedData.City) : null;
            State = deserializedData.State != null ? new CipherString(deserializedData.State) : null;
            PostalCode = deserializedData.PostalCode != null ? new CipherString(deserializedData.PostalCode) : null;
            Country = deserializedData.Country != null ? new CipherString(deserializedData.Country) : null;
            Company = deserializedData.Company != null ? new CipherString(deserializedData.Company) : null;
            Email = deserializedData.Email != null ? new CipherString(deserializedData.Email) : null;
            Phone = deserializedData.Phone != null ? new CipherString(deserializedData.Phone) : null;
            SSN = deserializedData.SSN != null ? new CipherString(deserializedData.SSN) : null;
            Username = deserializedData.Username != null ? new CipherString(deserializedData.Username) : null;
            PassportNumber = deserializedData.PassportNumber != null ?
                new CipherString(deserializedData.PassportNumber) : null;
            LicenseNumber = deserializedData.LicenseNumber != null ?
                new CipherString(deserializedData.LicenseNumber) : null;
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
    }
}
