using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace Bit.App.Models.View
{
    public class VaultView
    {
        public class Site
        {
            public Site(Models.Site site)
            {
                Id = site.Id;
                Name = site.Name?.Decrypt();
                Username = site.Username?.Decrypt();
            }

            public string Id { get; set; }
            public string Name { get; set; }
            public string Username { get; set; }
        }

        public class Folder : ObservableCollection<Site>
        {
            public Folder(string name) { Name = name; }

            public Folder(IEnumerable<Models.Site> sites)
            {
                Name = "(none)";
                foreach(var site in sites)
                {
                    Items.Add(new Site(site));
                }
            }

            public Folder(Models.Folder folder, IEnumerable<Models.Site> sites)
                : this(sites)
            {
                Id = folder.Id;
                Name = folder.Name?.Decrypt();
            }

            public string Id { get; set; }
            public string Name { get; set; }
            public string FirstLetter { get { return Name.Substring(0, 1); } }
        }
    }
}
