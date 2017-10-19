using System;
using System.Collections.Generic;
using Bit.App.Resources;
using System.Linq;

namespace Bit.App.Models.Page
{
    public class VaultListPageModel
    {
        public class Login
        {
            public Login(Models.Cipher cipher)
            {
                Id = cipher.Id;
                Shared = !string.IsNullOrWhiteSpace(cipher.OrganizationId);
                HasAttachments = cipher.Attachments?.Any() ?? false;
                FolderId = cipher.FolderId;
                Name = cipher.Name?.Decrypt(cipher.OrganizationId);
                Username = cipher.Login?.Username?.Decrypt(cipher.OrganizationId) ?? " ";
                Password = new Lazy<string>(() => cipher.Login?.Password?.Decrypt(cipher.OrganizationId));
                Uri = new Lazy<string>(() => cipher.Login?.Uri?.Decrypt(cipher.OrganizationId));
                Totp = new Lazy<string>(() => cipher.Login?.Totp?.Decrypt(cipher.OrganizationId));
            }

            public string Id { get; set; }
            public bool Shared { get; set; }
            public bool HasAttachments { get; set; }
            public string FolderId { get; set; }
            public string Name { get; set; }
            public string Username { get; set; }
            public Lazy<string> Password { get; set; }
            public Lazy<string> Uri { get; set; }
            public Lazy<string> Totp { get; set; }
        }

        public class AutofillLogin : Login
        {
            public AutofillLogin(Models.Cipher login, bool fuzzy = false)
                : base(login)
            {
                Fuzzy = fuzzy;
            }

            public bool Fuzzy { get; set; }
        }

        public class Folder : List<Login>
        {
            public Folder(Models.Folder folder)
            {
                Id = folder.Id;
                Name = folder.Name?.Decrypt();
            }

            public Folder(List<Login> logins)
            {
                AddRange(logins);
            }

            public string Id { get; set; }
            public string Name { get; set; } = AppResources.FolderNone;
        }

        public class AutofillGrouping : List<AutofillLogin>
        {
            public AutofillGrouping(List<AutofillLogin> logins, string name)
            {
                AddRange(logins);
                Name = name;
            }

            public string Name { get; set; }
        }
    }
}
