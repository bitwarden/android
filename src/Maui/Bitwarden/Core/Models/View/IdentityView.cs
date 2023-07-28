using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class IdentityView : ItemView
    {
        private string _firstName;
        private string _lastName;
        private string _subTitle;

        public IdentityView() { }

        public IdentityView(Identity i) { }

        public string Title { get; set; }
        public string FirstName
        {
            get => _firstName;
            set
            {
                _firstName = value;
                _subTitle = null;
            }
        }
        public string MiddleName { get; set; }
        public string LastName
        {
            get => _lastName;
            set
            {
                _lastName = value;
                _subTitle = null;
            }
        }
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

        public override string SubTitle
        {
            get
            {
                if (_subTitle == null && (FirstName != null || LastName != null))
                {
                    _subTitle = string.Empty;
                    if (FirstName != null)
                    {
                        _subTitle = FirstName;
                    }
                    if (LastName != null)
                    {
                        if (_subTitle != string.Empty)
                        {
                            _subTitle += " ";
                        }
                        _subTitle += LastName;
                    }
                }
                return _subTitle;
            }
        }

        public string FullName
        {
            get
            {
                if (!string.IsNullOrWhiteSpace(Title) || !string.IsNullOrWhiteSpace(FirstName) ||
                    !string.IsNullOrWhiteSpace(MiddleName) || !string.IsNullOrWhiteSpace(LastName))
                {
                    var name = string.Empty;
                    if (!string.IsNullOrWhiteSpace(Title))
                    {
                        name = string.Concat(name, Title, " ");
                    }
                    if (!string.IsNullOrWhiteSpace(FirstName))
                    {
                        name = string.Concat(name, FirstName, " ");
                    }
                    if (!string.IsNullOrWhiteSpace(MiddleName))
                    {
                        name = string.Concat(name, MiddleName, " ");
                    }
                    if (!string.IsNullOrWhiteSpace(LastName))
                    {
                        name = string.Concat(name, LastName);
                    }
                    return name.Trim();
                }
                return null;
            }
        }

        public string FullAddress
        {
            get
            {
                var address = Address1;
                if (!string.IsNullOrWhiteSpace(Address2))
                {
                    if (!string.IsNullOrWhiteSpace(address))
                    {
                        address += ", ";
                    }
                    address += Address2;
                }
                if (!string.IsNullOrWhiteSpace(Address3))
                {
                    if (!string.IsNullOrWhiteSpace(address))
                    {
                        address += ", ";
                    }
                    address += Address3;
                }
                return address;
            }
        }

        public string FullAddressPart2
        {
            get
            {
                if (string.IsNullOrWhiteSpace(City) && string.IsNullOrWhiteSpace(State) &&
                    string.IsNullOrWhiteSpace(PostalCode))
                {
                    return null;
                }
                var city = string.IsNullOrWhiteSpace(City) ? "-" : City;
                var state = string.IsNullOrWhiteSpace(State) ? "-" : State;
                var postalCode = string.IsNullOrWhiteSpace(PostalCode) ? "-" : PostalCode;
                return string.Format("{0}, {1}, {2}", city, state, postalCode);
            }
        }

        public override List<KeyValuePair<string, LinkedIdType>> LinkedFieldOptions
        {
            get => new List<KeyValuePair<string, LinkedIdType>>()
            {
                new KeyValuePair<string, LinkedIdType>("Title", LinkedIdType.Identity_Title),
                new KeyValuePair<string, LinkedIdType>("MiddleName", LinkedIdType.Identity_MiddleName),
                new KeyValuePair<string, LinkedIdType>("Address1", LinkedIdType.Identity_Address1),
                new KeyValuePair<string, LinkedIdType>("Address2", LinkedIdType.Identity_Address2),
                new KeyValuePair<string, LinkedIdType>("Address3", LinkedIdType.Identity_Address3),
                new KeyValuePair<string, LinkedIdType>("CityTown", LinkedIdType.Identity_City),
                new KeyValuePair<string, LinkedIdType>("StateProvince", LinkedIdType.Identity_State),
                new KeyValuePair<string, LinkedIdType>("ZipPostalCode", LinkedIdType.Identity_PostalCode),
                new KeyValuePair<string, LinkedIdType>("Country", LinkedIdType.Identity_Country),
                new KeyValuePair<string, LinkedIdType>("Company", LinkedIdType.Identity_Company),
                new KeyValuePair<string, LinkedIdType>("Email", LinkedIdType.Identity_Email),
                new KeyValuePair<string, LinkedIdType>("Phone", LinkedIdType.Identity_Phone),
                new KeyValuePair<string, LinkedIdType>("SSN", LinkedIdType.Identity_Ssn),
                new KeyValuePair<string, LinkedIdType>("Username", LinkedIdType.Identity_Username),
                new KeyValuePair<string, LinkedIdType>("PassportNumber", LinkedIdType.Identity_PassportNumber),
                new KeyValuePair<string, LinkedIdType>("LicenseNumber", LinkedIdType.Identity_LicenseNumber),
                new KeyValuePair<string, LinkedIdType>("FirstName", LinkedIdType.Identity_FirstName),
                new KeyValuePair<string, LinkedIdType>("LastName", LinkedIdType.Identity_LastName),
                new KeyValuePair<string, LinkedIdType>("FullName", LinkedIdType.Identity_FullName),
            };
        }
    }
}
