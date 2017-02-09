using System;
using Bit.App.Models;
using Foundation;

namespace Bit.iOS.Extension.Models
{
    public class Context
    {
        private DomainName _domainName;

        public NSExtensionContext ExtContext { get; set; }
        public string ProviderType { get; set; }
        public Uri Uri
        {
            get
            {
                Uri uri;
                if(string.IsNullOrWhiteSpace(UrlString) || !Uri.TryCreate(UrlString, UriKind.Absolute, out uri))
                {
                    return null;
                }

                return uri;
            }
        }
        public string UrlString { get; set; }
        public string LoginTitle { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string OldPassword { get; set; }
        public string Notes { get; set; }
        public PasswordGenerationOptions PasswordOptions { get; set; }
        public PageDetails Details { get; set; }
    }
}
