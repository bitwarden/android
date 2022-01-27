using Foundation;
using Bit.iOS.Core.Models;

namespace Bit.iOS.ShareExtension.Models
{
    public class Context : AppExtensionContext
    {
        public NSExtensionContext ExtensionContext { get; set; }
        public string ProviderType { get; set; }
        public string LoginTitle { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string OldPassword { get; set; }
        public string Notes { get; set; }
        public PageDetails Details { get; set; }
    }
}
