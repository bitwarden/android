using AuthenticationServices;
using Bit.iOS.Core.Models;
using Foundation;

namespace Bit.iOS.Autofill.Models
{
    public class Context : AppExtensionContext
    {
        public NSExtensionContext ExtContext { get; set; }
        public ASCredentialServiceIdentifier[] ServiceIdentifiers { get; set; }
        public ASPasswordCredentialIdentity CredentialIdentity { get; set; }
        public bool Configuring { get; set; }
    }
}
