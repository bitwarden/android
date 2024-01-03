using AuthenticationServices;
using Bit.iOS.Core.Models;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill.Models
{
    public class Context : AppExtensionContext
    {
        public NSExtensionContext ExtContext { get; set; }
        public ASCredentialServiceIdentifier[] ServiceIdentifiers { get; set; }
        public ASPasswordCredentialIdentity PasswordCredentialIdentity { get; set; }
        public ASPasskeyCredentialRequest PasskeyCredentialRequest { get; set; }
        public bool Configuring { get; set; }

        public ASPasskeyCredentialIdentity PasskeyCredentialIdentity
        {
            get
            {
                if (UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
                {
                    return PasskeyCredentialRequest?.CredentialIdentity as ASPasskeyCredentialIdentity;
                }
                return null;
            }
        }

        public string? RecordIdentifier
        {
            get
            {
                if (PasswordCredentialIdentity?.RecordIdentifier is string id)
                {
                    return id;
                }

                if (UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
                {
                    return PasskeyCredentialIdentity?.RecordIdentifier;
                }

                return null;
            }
        }

        public bool IsPasskey => PasskeyCredentialRequest != null;
    }
}
