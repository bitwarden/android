using System;
using System.Collections.Generic;
using Bit.App.Resources;
using System.Linq;
using Bit.App.Enums;

namespace Bit.App.Models.Page
{
    public class VaultListPageModel
    {
        public class Cipher
        {
            public Cipher(Models.Cipher cipher)
            {
                Id = cipher.Id;
                Shared = !string.IsNullOrWhiteSpace(cipher.OrganizationId);
                HasAttachments = cipher.Attachments?.Any() ?? false;
                FolderId = cipher.FolderId;
                Name = cipher.Name?.Decrypt(cipher.OrganizationId);
                Type = cipher.Type;

                switch(cipher.Type)
                {
                    case CipherType.Login:
                        Username = cipher.Login?.Username?.Decrypt(cipher.OrganizationId) ?? " ";
                        Password = new Lazy<string>(() => cipher.Login?.Password?.Decrypt(cipher.OrganizationId));
                        Uri = new Lazy<string>(() => cipher.Login?.Uri?.Decrypt(cipher.OrganizationId));
                        Totp = new Lazy<string>(() => cipher.Login?.Totp?.Decrypt(cipher.OrganizationId));

                        Subtitle = Username;
                        break;
                    case CipherType.SecureNote:
                        Subtitle = " ";
                        break;
                    case CipherType.Card:
                        var cardNumber = cipher.Card?.Number?.Decrypt(cipher.OrganizationId) ?? " ";
                        var cardBrand = cipher.Card?.Brand?.Decrypt(cipher.OrganizationId) ?? " ";
                        CardCode = new Lazy<string>(() => cipher.Card?.Code?.Decrypt(cipher.OrganizationId));

                        Subtitle = $"{cardBrand}, *{cardNumber}";
                        break;
                    case CipherType.Identity:
                        var firstName = cipher.Identity?.FirstName?.Decrypt(cipher.OrganizationId) ?? " ";
                        var lastName = cipher.Identity?.LastName?.Decrypt(cipher.OrganizationId) ?? " ";
                        Subtitle = $"{firstName} {lastName}";
                        break;
                    default:
                        break;
                }
            }

            public string Id { get; set; }
            public bool Shared { get; set; }
            public bool HasAttachments { get; set; }
            public string FolderId { get; set; }
            public string Name { get; set; }
            public string Subtitle { get; set; }
            public CipherType Type { get; set; }

            // Login metadata
            public string Username { get; set; }
            public Lazy<string> Password { get; set; }
            public Lazy<string> Uri { get; set; }
            public Lazy<string> Totp { get; set; }

            // Login metadata
            public string CardNumber { get; set; }
            public Lazy<string> CardCode { get; set; }
        }

        public class AutofillCipher : Cipher
        {
            public AutofillCipher(Models.Cipher cipher, bool fuzzy = false)
                : base(cipher)
            {
                Fuzzy = fuzzy;
            }

            public bool Fuzzy { get; set; }
        }

        public class Folder : List<Cipher>
        {
            public Folder(Models.Folder folder)
            {
                Id = folder.Id;
                Name = folder.Name?.Decrypt();
            }

            public Folder(List<Cipher> ciphers)
            {
                AddRange(ciphers);
            }

            public string Id { get; set; }
            public string Name { get; set; } = AppResources.FolderNone;
        }

        public class AutofillGrouping : List<AutofillCipher>
        {
            public AutofillGrouping(List<AutofillCipher> logins, string name)
            {
                AddRange(logins);
                Name = name;
            }

            public string Name { get; set; }
        }
    }
}
