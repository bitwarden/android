using AuthenticationServices;
using Foundation;

namespace Bit.iOS.Autofill.Models
{
    public class Context
    {
        public NSExtensionContext ExtContext { get; set; }
        public ASCredentialServiceIdentifier[] ServiceIdentifiers { get; set; }
        public string UrlString { get; set; }
    }
}
