using Bit.Core.Models.Domain;
using System.Collections.Generic;

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

        public override List<KeyValuePair<string, int>> LinkedMetadata
        {
            get => new List<KeyValuePair<string, int>>()
            {
                new KeyValuePair<string, int>("Title", 0),
                new KeyValuePair<string, int>("MiddleName", 1),
                new KeyValuePair<string, int>("Address1", 2),
                new KeyValuePair<string, int>("Address2", 3),
                new KeyValuePair<string, int>("Address3", 4),
                new KeyValuePair<string, int>("CityTown", 5),
                new KeyValuePair<string, int>("StateProvince", 6),
                new KeyValuePair<string, int>("ZipPostalCode", 7),
                new KeyValuePair<string, int>("Country", 8),
                new KeyValuePair<string, int>("Company", 9),
                new KeyValuePair<string, int>("Email", 10),
                new KeyValuePair<string, int>("Phone", 11),
                new KeyValuePair<string, int>("SSN", 12),
                new KeyValuePair<string, int>("Username", 13),
                new KeyValuePair<string, int>("PassportNumber", 14),
                new KeyValuePair<string, int>("LicenseNumber", 15),
                new KeyValuePair<string, int>("FirstName", 16),
                new KeyValuePair<string, int>("LastName", 17),
                new KeyValuePair<string, int>("FullName", 18),
            };
        }
    }
}
