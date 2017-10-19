using Bit.App.Enums;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Models.Api
{
    public class CipherRequest
    {
        public CipherRequest(Cipher cipher)
        {
            Type = cipher.Type;
            OrganizationId = cipher.OrganizationId;
            FolderId = cipher.FolderId;
            Name = cipher.Name?.EncryptedString;
            Notes = cipher.Notes?.EncryptedString;
            Favorite = cipher.Favorite;

            if(cipher.Fields != null)
            {
                Fields = cipher.Fields.Select(f => new FieldDataModel
                {
                    Name = f.Name?.EncryptedString,
                    Value = f.Value?.EncryptedString,
                    Type = f.Type
                });
            }

            switch(Type)
            {
                case CipherType.Login:
                    Login = new LoginType(cipher);
                    break;
                case CipherType.Card:
                    Card = new CardType(cipher);
                    break;
                case CipherType.Identity:
                    Identity = new IdentityType(cipher);
                    break;
                case CipherType.SecureNote:
                    SecureNote = new SecureNoteType(cipher);
                    break;
                default:
                    break;
            }
        }

        public CipherType Type { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public bool Favorite { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public IEnumerable<FieldDataModel> Fields { get; set; }

        public LoginType Login { get; set; }
        public CardType Card { get; set; }
        public IdentityType Identity { get; set; }
        public SecureNoteType SecureNote { get; set; }

        public class LoginType
        {
            public LoginType(Cipher cipher)
            {
                Uri = cipher.Login.Uri?.EncryptedString;
                Username = cipher.Login.Username?.EncryptedString;
                Password = cipher.Login.Password?.EncryptedString;
                Totp = cipher.Login.Totp?.EncryptedString;
            }

            public string Uri { get; set; }
            public string Username { get; set; }
            public string Password { get; set; }
            public string Totp { get; set; }
        }

        public class CardType
        {
            public CardType(Cipher cipher)
            {
                CardholderName = cipher.Card.CardholderName?.EncryptedString;
                Brand = cipher.Card.Brand?.EncryptedString;
                Number = cipher.Card.Number?.EncryptedString;
                ExpMonth = cipher.Card.ExpMonth?.EncryptedString;
                ExpYear = cipher.Card.ExpYear?.EncryptedString;
                Code = cipher.Card.Code?.EncryptedString;
            }

            public string CardholderName { get; set; }
            public string Brand { get; set; }
            public string Number { get; set; }
            public string ExpMonth { get; set; }
            public string ExpYear { get; set; }
            public string Code { get; set; }
        }

        public class IdentityType
        {
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

        public class SecureNoteType
        {
            public SecureNoteType(Cipher cipher)
            {
                Type = cipher.SecureNote.Type;
            }

            public Enums.SecureNoteType Type { get; set; }
        }
    }
}
