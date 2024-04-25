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
        public ASPasskeyCredentialRequestParameters PasskeyCredentialRequestParameters { get; set; }
        public bool Configuring { get; set; }
        public bool IsExecutingWithoutUserInteraction { get; set; }

        public bool IsCreatingPasskey { get; set; }
        public Fido2ConfirmNewCredentialParams? PasskeyCreationParams { get; set; }
        /// <summary>
        /// This is used to defer the completion until the vault is unlocked.
        /// </summary>
        public TaskCompletionSource<bool> UnlockVaultTcs { get; set; }
        /// <summary>
        /// This is used to defer the completion until a vault item is chosen to add the passkey to.
        /// Param: cipher ID to add the passkey to.
        /// Param: isUserVerified if the user was verified. If null then the verification hasn't been done.
        /// </summary>
        public TaskCompletionSource<(string cipherId, bool? isUserVerified)> PickCredentialForFido2CreationTcs { get; set; }
        /// <summary>
        /// This is used to defer the completion until a vault item is chosen to use the passkey.
        /// Param: cipher ID to add the passkey to.
        /// </summary>
        public TaskCompletionSource<string> PickCredentialForFido2GetAssertionFromListTcs { get; set; }

        public bool VaultUnlockedDuringThisSession { get; set; }

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

        public bool IsPasswordFallback { get; set; }

        public bool IsPreparingListForPasskey => PasskeyCredentialRequestParameters != null && !IsPasswordFallback;

        public bool IsCreatingOrPreparingListForPasskey => IsCreatingPasskey || IsPreparingListForPasskey;
    }
}
