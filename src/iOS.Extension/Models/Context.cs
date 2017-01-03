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
        public Uri Url { get; set; }
        public DomainName DomainName
        {
            get
            {
                if(_domainName != null)
                {
                    return _domainName;
                }

                DomainName domain;
                if(Url?.Host != null && DomainName.TryParse(Url?.Host, out domain))
                {
                    _domainName = domain;
                }

                return _domainName;
            }
        }
        public string LoginTitle { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string OldPassword { get; set; }
        public string Notes { get; set; }
        public PasswordGenerationOptions PasswordOptions { get; set; }
        public PageDetails Details { get; set; }
    }
}
