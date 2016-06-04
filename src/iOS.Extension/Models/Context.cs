using System;
using Foundation;

namespace Bit.iOS.Extension.Models
{
    public class Context
    {
        public NSExtensionContext ExtContext { get; set; }
        public string ProviderType { get; set; }
        public Uri Url { get; set; }
        public string SiteTitle { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string OldPassword { get; set; }
        public string Notes { get; set; }
        public PasswordGenerationOptions PasswordOptions { get; set; }
        public PageDetails Details { get; set; }
    }
}
