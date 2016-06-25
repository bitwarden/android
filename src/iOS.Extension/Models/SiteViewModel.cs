using System;
using System.Collections.Generic;
using System.Text;
using Bit.App.Models;

namespace Bit.iOS.Extension.Models
{
    public class SiteViewModel
    {
        public SiteViewModel(Site site)
        {
            Id = site.Id;
            Name = site.Name?.Decrypt();
            Username = site.Username?.Decrypt();
            Password = site.Password?.Decrypt();
            Uri = site.Uri?.Decrypt();
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Uri { get; set; }
        public string HostName
        {
            get
            {
                if(string.IsNullOrWhiteSpace(Uri))
                {
                    return null;
                }

                try
                {
                    return new Uri(Uri)?.Host;
                }
                catch
                {
                    return null;
                };
            }
        }
    }
}
