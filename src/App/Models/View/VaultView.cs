using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace Bit.App.Models.View
{
    public class VaultView
    {
        public class Site
        {
            public Site(Models.Site site, string folderId)
            {
                Id = site.Id;
                FolderId = folderId;
                Name = site.Name?.Decrypt();
                Username = site.Username?.Decrypt();
                Password = site.Password?.Decrypt();
                Uri = site.Uri?.Decrypt();
            }

            public string Id { get; set; }
            public string FolderId { get; set; }
            public string Name { get; set; }
            public string Username { get; set; }
            public string Password { get; set; }
            public string Uri { get; set; }
        }

        public class Folder : ObservableCollection<Site>
        {
            public Folder(IEnumerable<Models.Site> sites, string folderId = null)
            {
                foreach(var site in sites)
                {
                    Items.Add(new Site(site, folderId));
                }
            }

            public Folder(Models.Folder folder, IEnumerable<Models.Site> sites)
                : this(sites, folder.Id)
            {
                Id = folder.Id;
                Name = folder.Name?.Decrypt();
            }

            public string Id { get; set; }
            public string Name { get; set; } = "(none)";
            public string FirstLetter { get { return Name.Substring(0, 1); } }
        }
    }
}
