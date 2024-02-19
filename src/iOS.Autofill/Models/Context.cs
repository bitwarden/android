using System.Threading.Tasks;
using AuthenticationServices;
using Bit.Core.Abstractions;
using Bit.iOS.Core.Models;
using Foundation;
using ObjCRuntime;
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
        public bool IsCreatingPasskey { get; set; }
        public TaskCompletionSource<bool> UnlockVaultTcs { get; set; }
        public TaskCompletionSource<Fido2ConfirmNewCredentialResult> ConfirmNewCredentialTcs { get; set; }

        public ASPasskeyCredentialIdentity PasskeyCredentialIdentity
        {
            get
            {
                if (PasskeyCredentialRequest != null && UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
                {
                    return Runtime.GetNSObject<ASPasskeyCredentialIdentity>(PasskeyCredentialRequest.CredentialIdentity.GetHandle());
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
