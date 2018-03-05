using System;
using System.ComponentModel;
using Xamarin.Forms;
using System.Collections.Generic;
using Bit.App.Enums;

namespace Bit.App.Models.Page
{
    public class VaultViewCipherPageModel : INotifyPropertyChanged
    {
        private string _name, _notes;
        private List<Attachment> _attachments;
        private List<Field> _fields;

        // Login
        private string _loginUsername, _loginPassword, _loginUri, _loginTotpCode;
        private int _loginTotpSec = 30;
        private bool _loginRevealPassword;

        // Card
        private string _cardName, _cardNumber, _cardBrand, _cardExpMonth, _cardExpYear, _cardCode;

        // Identity
        private string _idFirstName, _idLastName, _idMiddleName, _idCompany, _idEmail, _idPhone, _idUsername,
            _idPassportNumber, _idLicenseNumber, _idSsn, _idAddress1, _idAddress2, _idAddress3, _idCity,
            _idState, _idCountry, _idPostalCode, _idTitle;

        public VaultViewCipherPageModel() { }

        public event PropertyChangedEventHandler PropertyChanged;

        public string Name
        {
            get => _name;
            set
            {
                _name = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Name)));
            }
        }

        public string Notes
        {
            get => _notes;
            set
            {
                _notes = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Notes)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowNotes)));
            }
        }
        public bool ShowNotes => !string.IsNullOrWhiteSpace(Notes);

        public List<Attachment> Attachments
        {
            get => _attachments;
            set
            {
                _attachments = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Attachments)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowAttachments)));
            }
        }
        public bool ShowAttachments => (Attachments?.Count ?? 0) > 0;

        public List<Field> Fields
        {
            get => _fields;
            set
            {
                _fields = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Fields)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowFields)));
            }
        }
        public bool ShowFields => (Fields?.Count ?? 0) > 0;

        // Login
        public string LoginUsername
        {
            get => _loginUsername;
            set
            {
                _loginUsername = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LoginUsername)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowLoginUsername)));
            }
        }
        public bool ShowLoginUsername => !string.IsNullOrWhiteSpace(LoginUsername);

        public string LoginPassword
        {
            get => _loginPassword;
            set
            {
                _loginPassword = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LoginPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(MaskedLoginPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowLoginPassword)));
            }
        }
        public bool ShowLoginPassword => !string.IsNullOrWhiteSpace(LoginPassword);
        public bool RevealLoginPassword
        {
            get => _loginRevealPassword;
            set
            {
                _loginRevealPassword = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(RevealLoginPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(MaskedLoginPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LoginShowHideImage)));
            }
        }
        public string MaskedLoginPassword => RevealLoginPassword ?
            LoginPassword : LoginPassword == null ? null : new string('•', LoginPassword.Length);
        public ImageSource LoginShowHideImage => RevealLoginPassword ?
            ImageSource.FromFile("eye_slash.png") : ImageSource.FromFile("eye.png");

        public string LoginUri
        {
            get => _loginUri;
            set
            {
                _loginUri = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LoginUri)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LoginUriHost)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowLoginUri)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowLoginLaunch)));
            }
        }
        public bool ShowLoginUri => !string.IsNullOrWhiteSpace(LoginUri);
        public bool ShowLoginLaunch
        {
            get
            {
                if(!ShowLoginUri)
                {
                    return false;
                }

                if(Device.RuntimePlatform == Device.Android && !LoginUri.StartsWith("http") &&
                    !LoginUri.StartsWith("androidapp://"))
                {
                    return false;
                }

                if(Device.RuntimePlatform != Device.Android && !LoginUri.StartsWith("http"))
                {
                    return false;
                }

                if(!Uri.TryCreate(LoginUri, UriKind.Absolute, out Uri uri))
                {
                    return false;
                }

                return true;
            }
        }
        public string LoginUriHost
        {
            get
            {
                if(!ShowLoginUri)
                {
                    return null;
                }

                if(!Uri.TryCreate(LoginUri, UriKind.Absolute, out Uri uri))
                {
                    return LoginUri;
                }

                if(DomainName.TryParseBaseDomain(uri.Host, out string domain))
                {
                    return domain;
                }

                return uri.Host;
            }
        }

        public string LoginTotpCode
        {
            get => _loginTotpCode;
            set
            {
                _loginTotpCode = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LoginTotpCode)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LoginTotpCodeFormatted)));
            }
        }
        public int LoginTotpSecond
        {
            get => _loginTotpSec;
            set
            {
                _loginTotpSec = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LoginTotpSecond)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(LoginTotpColor)));
            }
        }
        public bool LoginTotpLow => LoginTotpSecond <= 7;
        public Color LoginTotpColor => !string.IsNullOrWhiteSpace(LoginTotpCode) && LoginTotpLow ?
            Color.Red : Color.Black;
        public string LoginTotpCodeFormatted => !string.IsNullOrWhiteSpace(LoginTotpCode) ?
            string.Format("{0} {1}", LoginTotpCode.Substring(0, 3), LoginTotpCode.Substring(3)) : null;

        // Card
        public string CardName
        {
            get => _cardName;
            set
            {
                _cardName = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(CardName)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowCardName)));
            }
        }
        public bool ShowCardName => !string.IsNullOrWhiteSpace(CardName);

        public string CardNumber
        {
            get => _cardNumber;
            set
            {
                _cardNumber = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(CardNumber)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowCardNumber)));
            }
        }
        public bool ShowCardNumber => !string.IsNullOrWhiteSpace(CardNumber);

        public string CardBrand
        {
            get => _cardBrand;
            set
            {
                _cardBrand = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(CardBrand)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowCardBrand)));
            }
        }
        public bool ShowCardBrand => !string.IsNullOrWhiteSpace(CardBrand);

        public string CardExpMonth
        {
            private get => _cardExpMonth;
            set
            {
                _cardExpMonth = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(CardExpMonth)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(CardExp)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowCardExp)));
            }
        }

        public string CardExpYear
        {
            private get => _cardExpYear;
            set
            {
                _cardExpYear = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(CardExpYear)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(CardExp)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowCardExp)));
            }
        }

        public string CardExp
        {
            get
            {
                var expMonth = !string.IsNullOrWhiteSpace(CardExpMonth) ? CardExpMonth.PadLeft(2, '0') : "__";
                var expYear = "____";
                if(!string.IsNullOrWhiteSpace(CardExpYear))
                {
                    expYear = CardExpYear;
                }
                if(expYear.Length == 2)
                {
                    expYear = "20" + expYear;
                }

                return $"{expMonth} / {expYear}";
            }
        }
        public bool ShowCardExp => !string.IsNullOrWhiteSpace(CardExpMonth) && !string.IsNullOrWhiteSpace(CardExpYear);

        public string CardCode
        {
            get => _cardCode;
            set
            {
                _cardCode = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(CardCode)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowCardCode)));
            }
        }
        public bool ShowCardCode => !string.IsNullOrWhiteSpace(CardCode);

        // Identity

        public string IdTitle
        {
            get => _idTitle;
            set
            {
                _idTitle = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdTitle)));
            }
        }
        public string IdFirstName
        {
            private get => _idFirstName;
            set
            {
                _idFirstName = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdFirstName)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdName)));
            }
        }
        public string IdMiddleName
        {
            private get => _idMiddleName;
            set
            {
                _idMiddleName = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdMiddleName)));
            }
        }
        public string IdLastName
        {
            private get => _idLastName;
            set
            {
                _idLastName = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdLastName)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdName)));
            }
        }
        public string IdName
        {
            get
            {
                var name = IdTitle;
                if(!string.IsNullOrWhiteSpace(IdFirstName))
                {
                    name += ((!string.IsNullOrWhiteSpace(name) ? " " : string.Empty) + IdFirstName);
                }
                if(!string.IsNullOrWhiteSpace(IdMiddleName))
                {
                    name += ((!string.IsNullOrWhiteSpace(name) ? " " : string.Empty) + IdMiddleName);
                }
                if(!string.IsNullOrWhiteSpace(IdLastName))
                {
                    name += ((!string.IsNullOrWhiteSpace(name) ? " " : string.Empty) + IdLastName);
                }
                return name;
            }
        }
        public bool ShowIdName => !string.IsNullOrWhiteSpace(IdFirstName) || !string.IsNullOrWhiteSpace(IdLastName);

        public string IdUsername
        {
            get => _idUsername;
            set
            {
                _idUsername = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdUsername)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdUsername)));
            }
        }
        public bool ShowIdUsername => !string.IsNullOrWhiteSpace(IdUsername);

        public string IdCompany
        {
            get => _idCompany;
            set
            {
                _idCompany = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdCompany)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdCompany)));
            }
        }
        public bool ShowIdCompany => !string.IsNullOrWhiteSpace(IdCompany);

        public string IdSsn
        {
            get => _idSsn;
            set
            {
                _idSsn = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdSsn)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdSsn)));
            }
        }
        public bool ShowIdSsn => !string.IsNullOrWhiteSpace(IdSsn);

        public string IdPassportNumber
        {
            get => _idPassportNumber;
            set
            {
                _idPassportNumber = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdPassportNumber)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdPassportNumber)));
            }
        }
        public bool ShowIdPassportNumber => !string.IsNullOrWhiteSpace(IdPassportNumber);

        public string IdLicenseNumber
        {
            get => _idLicenseNumber;
            set
            {
                _idLicenseNumber = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdLicenseNumber)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdLicenseNumber)));
            }
        }
        public bool ShowIdLicenseNumber => !string.IsNullOrWhiteSpace(IdLicenseNumber);

        public string IdEmail
        {
            get => _idEmail;
            set
            {
                _idEmail = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdEmail)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdEmail)));
            }
        }
        public bool ShowIdEmail => !string.IsNullOrWhiteSpace(IdEmail);

        public string IdPhone
        {
            get => _idPhone;
            set
            {
                _idPhone = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdPhone)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdPhone)));
            }
        }
        public bool ShowIdPhone => !string.IsNullOrWhiteSpace(IdPhone);

        public string IdAddress1
        {
            get => _idAddress1;
            set
            {
                _idAddress1 = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress1)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdAddress)));
            }
        }
        public string IdAddress2
        {
            get => _idAddress2;
            set
            {
                _idAddress2 = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress2)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress)));
            }
        }
        public string IdAddress3
        {
            get => _idAddress3;
            set
            {
                _idAddress3 = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress3)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress)));
            }
        }
        public string IdCity
        {
            get => _idCity;
            set
            {
                _idCity = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdCity)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdAddress)));
            }
        }
        public string IdState
        {
            get => _idState;
            set
            {
                _idState = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdState)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress)));
            }
        }
        public string IdPostalCode
        {
            get => _idPostalCode;
            set
            {
                _idPostalCode = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdPostalCode)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress)));
            }
        }
        public string IdCountry
        {
            get => _idCountry;
            set
            {
                _idCountry = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdCountry)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(IdAddress)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowIdAddress)));
            }
        }
        public string IdAddress
        {
            get
            {
                var address = IdAddress1;
                if(!string.IsNullOrWhiteSpace(IdAddress2))
                {
                    address += ((!string.IsNullOrWhiteSpace(address) ? "\n" : string.Empty) + IdAddress2);
                }
                if(!string.IsNullOrWhiteSpace(IdAddress3))
                {
                    address += ((!string.IsNullOrWhiteSpace(address) ? "\n" : string.Empty) + IdAddress3);
                }
                if(!string.IsNullOrWhiteSpace(IdCity) || !string.IsNullOrWhiteSpace(IdState) ||
                    !string.IsNullOrWhiteSpace(IdPostalCode))
                {
                    var cityLine = IdCity + ", ";
                    cityLine += !string.IsNullOrWhiteSpace(IdState) ? IdState : "-";
                    cityLine += " ";
                    cityLine += !string.IsNullOrWhiteSpace(IdPostalCode) ? IdPostalCode : "-";
                    address += ((!string.IsNullOrWhiteSpace(address) ? "\n" : string.Empty) + cityLine);
                }
                if(!string.IsNullOrWhiteSpace(IdCountry))
                {
                    address += ((!string.IsNullOrWhiteSpace(address) ? "\n" : string.Empty) + IdCountry);
                }
                return address;
            }
        }
        public bool ShowIdAddress => !string.IsNullOrWhiteSpace(IdAddress1) || !string.IsNullOrWhiteSpace(IdCity) ||
            !string.IsNullOrWhiteSpace(IdCountry);

        public void Update(Cipher cipher)
        {
            Name = cipher.Name?.Decrypt(cipher.OrganizationId);
            Notes = cipher.Notes?.Decrypt(cipher.OrganizationId);

            if(cipher.Attachments != null)
            {
                var attachments = new List<Attachment>();
                foreach(var attachment in cipher.Attachments)
                {
                    attachments.Add(new Attachment
                    {
                        Id = attachment.Id,
                        Name = attachment.FileName?.Decrypt(cipher.OrganizationId),
                        SizeName = attachment.SizeName,
                        Size = attachment.Size,
                        Url = attachment.Url
                    });
                }
                Attachments = attachments;
            }
            else
            {
                cipher.Attachments = null;
            }

            if(cipher.Fields != null)
            {
                var fields = new List<Field>();
                foreach(var field in cipher.Fields)
                {
                    fields.Add(new Field
                    {
                        Name = field.Name?.Decrypt(cipher.OrganizationId),
                        Value = field.Value?.Decrypt(cipher.OrganizationId),
                        Type = field.Type
                    });
                }
                Fields = fields;
            }
            else
            {
                Fields = null;
            }

            switch(cipher.Type)
            {
                case CipherType.Login:
                    LoginUsername = cipher.Login.Username?.Decrypt(cipher.OrganizationId);
                    LoginPassword = cipher.Login.Password?.Decrypt(cipher.OrganizationId);
                    LoginUri = cipher.Login.Uri?.Decrypt(cipher.OrganizationId);
                    break;
                case CipherType.Card:
                    CardName = cipher.Card.CardholderName?.Decrypt(cipher.OrganizationId);
                    CardNumber = cipher.Card.Number?.Decrypt(cipher.OrganizationId);
                    CardBrand = cipher.Card.Brand?.Decrypt(cipher.OrganizationId);
                    CardExpMonth = cipher.Card.ExpMonth?.Decrypt(cipher.OrganizationId);
                    CardExpYear = cipher.Card.ExpYear?.Decrypt(cipher.OrganizationId);
                    CardCode = cipher.Card.Code?.Decrypt(cipher.OrganizationId);
                    break;
                case CipherType.Identity:
                    IdTitle = cipher.Identity.Title?.Decrypt(cipher.OrganizationId);
                    IdFirstName = cipher.Identity.FirstName?.Decrypt(cipher.OrganizationId);
                    IdMiddleName = cipher.Identity.MiddleName?.Decrypt(cipher.OrganizationId);
                    IdLastName = cipher.Identity.LastName?.Decrypt(cipher.OrganizationId);
                    IdCompany = cipher.Identity.Company?.Decrypt(cipher.OrganizationId);
                    IdUsername = cipher.Identity.Username?.Decrypt(cipher.OrganizationId);
                    IdSsn = cipher.Identity.SSN?.Decrypt(cipher.OrganizationId);
                    IdPassportNumber = cipher.Identity.PassportNumber?.Decrypt(cipher.OrganizationId);
                    IdLicenseNumber = cipher.Identity.LicenseNumber?.Decrypt(cipher.OrganizationId);
                    IdEmail = cipher.Identity.Email?.Decrypt(cipher.OrganizationId);
                    IdPhone = cipher.Identity.Phone?.Decrypt(cipher.OrganizationId);
                    IdAddress1 = cipher.Identity.Address1?.Decrypt(cipher.OrganizationId);
                    IdAddress2 = cipher.Identity.Address2?.Decrypt(cipher.OrganizationId);
                    IdAddress3 = cipher.Identity.Address3?.Decrypt(cipher.OrganizationId);
                    IdCity = cipher.Identity.City?.Decrypt(cipher.OrganizationId);
                    IdState = cipher.Identity.State?.Decrypt(cipher.OrganizationId);
                    IdPostalCode = cipher.Identity.PostalCode?.Decrypt(cipher.OrganizationId);
                    IdCountry = cipher.Identity.Country?.Decrypt(cipher.OrganizationId);
                    break;
                default:
                    break;
            }
        }

        public class Attachment
        {
            public string Id { get; set; }
            public string Name { get; set; }
            public string SizeName { get; set; }
            public long Size { get; set; }
            public string Url { get; set; }
        }

        public class Field
        {
            private string _maskedValue;

            public string Name { get; set; }
            public string Value { get; set; }
            public string MaskedValue
            {
                get
                {
                    if(_maskedValue == null && Value != null)
                    {
                        _maskedValue = new string('•', Value.Length);
                    }

                    return _maskedValue;
                }
            }
            public FieldType Type { get; set; }
            public bool Revealed { get; set; }
        }
    }
}
