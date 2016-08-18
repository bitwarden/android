using System;
using System.Collections.Generic;
using Bit.App.Resources;

namespace Bit.App.Models.Page
{
    public class VaultListPageModel
    {
        public class Site
        {
            public Site(Models.Site site)
            {
                Id = site.Id;
                FolderId = site.FolderId;
                Name = site.Name?.Decrypt();
                Username = site.Username?.Decrypt() ?? " ";
                Password = new Lazy<string>(() => site.Password?.Decrypt());
                Uri = new Lazy<string>(() => site.Uri?.Decrypt());
            }

            public string Id { get; set; }
            public string FolderId { get; set; }
            public string Name { get; set; }
            public string Username { get; set; }
            public Lazy<string> Password { get; set; }
            public Lazy<string> Uri { get; set; }
        }

        public class Folder : List<Site>
        {
            public Folder(Models.Folder folder)
            {
                Id = folder.Id;
                Name = folder.Name?.Decrypt();
            }

            public Folder(List<Site> sites)
            {
                AddRange(sites);
            }

            public string Id { get; set; }
            public string Name { get; set; } = AppResources.FolderNone;
        }
    }
}
