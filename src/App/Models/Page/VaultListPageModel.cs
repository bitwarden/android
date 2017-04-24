using System;
using System.Collections.Generic;
using Bit.App.Resources;

namespace Bit.App.Models.Page
{
    public class VaultListPageModel
    {
        public class Login
        {
            public Login(Models.Login login)
            {
                Id = login.Id;
                Shared = !string.IsNullOrWhiteSpace(login.OrganizationId);
                FolderId = login.FolderId;
                Name = login.Name?.Decrypt(login.OrganizationId);
                Username = login.Username?.Decrypt(login.OrganizationId) ?? " ";
                Password = new Lazy<string>(() => login.Password?.Decrypt(login.OrganizationId));
                Uri = new Lazy<string>(() => login.Uri?.Decrypt(login.OrganizationId));
            }

            public string Id { get; set; }
            public bool Shared { get; set; }
            public string FolderId { get; set; }
            public string Name { get; set; }
            public string Username { get; set; }
            public Lazy<string> Password { get; set; }
            public Lazy<string> Uri { get; set; }
        }

        public class AutofillLogin : Login
        {
            public AutofillLogin(Models.Login login, bool fuzzy = false)
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
