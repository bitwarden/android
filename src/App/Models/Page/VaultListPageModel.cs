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
                FolderId = login.FolderId;
                Name = login.Name?.Decrypt();
                Username = login.Username?.Decrypt() ?? " ";
                Password = new Lazy<string>(() => login.Password?.Decrypt());
                Uri = new Lazy<string>(() => login.Uri?.Decrypt());
            }

            public string Id { get; set; }
            public string FolderId { get; set; }
            public string Name { get; set; }
            public string Username { get; set; }
            public Lazy<string> Password { get; set; }
            public Lazy<string> Uri { get; set; }
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
    }
}
