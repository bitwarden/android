using Bit.Core.Enums;
using Bit.Core.Models.Api;
using Bit.Core.Models.Domain;
using System.Collections.Generic;
using System.Linq;
using System;

namespace Bit.Core.Models.Request
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
            LastKnownRevisionDate = cipher.RevisionDate;
            Reprompt = cipher.Reprompt;

            switch (Type)
            {
                case CipherType.Login:
                    Login = new LoginApi
                    {
                        Uris = cipher.Login.Uris?.Select(
                            u => new LoginUriApi { Match = u.Match, Uri = u.Uri?.EncryptedString }).ToList(),
                        Username = cipher.Login.Username?.EncryptedString,
                        Password = cipher.Login.Password?.EncryptedString,
                        PasswordRevisionDate = cipher.Login.PasswordRevisionDate,
                        Totp = cipher.Login.Totp?.EncryptedString
                    };
                    break;
                case CipherType.Card:
                    Card = new CardApi
                    {
                        CardholderName = cipher.Card.CardholderName?.EncryptedString,
                        Brand = cipher.Card.Brand?.EncryptedString,
                        Number = cipher.Card.Number?.EncryptedString,
                        ExpMonth = cipher.Card.ExpMonth?.EncryptedString,
                        ExpYear = cipher.Card.ExpYear?.EncryptedString,
                        Code = cipher.Card.Code?.EncryptedString
                    };
                    break;
                case CipherType.Identity:
                    Identity = new IdentityApi
                    {
                        Title = cipher.Identity.Title?.EncryptedString,
                        FirstName = cipher.Identity.FirstName?.EncryptedString,
                        MiddleName = cipher.Identity.MiddleName?.EncryptedString,
                        LastName = cipher.Identity.LastName?.EncryptedString,
                        Address1 = cipher.Identity.Address1?.EncryptedString,
                        Address2 = cipher.Identity.Address2?.EncryptedString,
                        Address3 = cipher.Identity.Address3?.EncryptedString,
                        City = cipher.Identity.City?.EncryptedString,
                        State = cipher.Identity.State?.EncryptedString,
                        PostalCode = cipher.Identity.PostalCode?.EncryptedString,
                        Country = cipher.Identity.Country?.EncryptedString,
                        Company = cipher.Identity.Company?.EncryptedString,
                        Email = cipher.Identity.Email?.EncryptedString,
                        Phone = cipher.Identity.Phone?.EncryptedString,
                        SSN = cipher.Identity.SSN?.EncryptedString,
                        Username = cipher.Identity.Username?.EncryptedString,
                        PassportNumber = cipher.Identity.PassportNumber?.EncryptedString,
                        LicenseNumber = cipher.Identity.LicenseNumber?.EncryptedString
                    };
                    break;
                case CipherType.SecureNote:
                    SecureNote = new SecureNoteApi
                    {
                        Type = cipher.SecureNote.Type
                    };
                    break;
                default:
                    break;
            }

            Fields = cipher.Fields?.Select(f => new FieldApi
            {
                Type = f.Type,
                Name = f.Name?.EncryptedString,
                Value = f.Value?.EncryptedString
            }).ToList();

            PasswordHistory = cipher.PasswordHistory?.Select(ph => new PasswordHistoryRequest
            {
                Password = ph.Password?.EncryptedString,
                LastUsedDate = ph.LastUsedDate
            }).ToList();

            if (cipher.Attachments != null)
            {
                Attachments = new Dictionary<string, string>();
                Attachments2 = new Dictionary<string, AttachmentRequest>();
                foreach (var attachment in cipher.Attachments)
                {
                    var fileName = attachment.FileName?.EncryptedString;
                    Attachments.Add(attachment.Id, fileName);
                    Attachments2.Add(attachment.Id, new AttachmentRequest
                    {
                        FileName = fileName,
                        Key = attachment.Key?.EncryptedString
                    });
                }
            }
        }

        public CipherType Type { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public bool Favorite { get; set; }
        public LoginApi Login { get; set; }
        public SecureNoteApi SecureNote { get; set; }
        public CardApi Card { get; set; }
        public IdentityApi Identity { get; set; }
        public List<FieldApi> Fields { get; set; }
        public List<PasswordHistoryRequest> PasswordHistory { get; set; }
        public Dictionary<string, string> Attachments { get; set; }
        public Dictionary<string, AttachmentRequest> Attachments2 { get; set; }
        public DateTime LastKnownRevisionDate { get; set; }
        public CipherRepromptType Reprompt { get; set; }
    }
}
