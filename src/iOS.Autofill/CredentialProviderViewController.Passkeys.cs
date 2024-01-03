using System;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController, IAccountsManagerHost
    {
        private async Task ProvideCredentialWithoutUserInteractionAsync(ASPasskeyCredentialRequest passkeyCredentialRequest)
        {
            InitAppIfNeeded();
            await _stateService.Value.SetPasswordRepromptAutofillAsync(false);
            await _stateService.Value.SetPasswordVerifiedAutofillAsync(false);
            if (!await IsAuthed() || await IsLocked())
            {
                CancelRequest(ASExtensionErrorCode.UserInteractionRequired);
                return;
            }
            _context.PasskeyCredentialRequest = passkeyCredentialRequest;
            await ProvideCredentialAsync(false);
        }

        public void CompleteAssertionRequest(CipherView cipherView)
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                OnProvidingCredentialException(new InvalidOperationException("Trying to complete assertion request before iOS 17"));
                return;
            }

            // TODO: Generate the credential Signature and Auth data accordingly
            CompleteAssertionRequest(new ASPasskeyAssertionCredential(
                cipherView.Login.MainFido2Credential.UserHandle,
                cipherView.Login.MainFido2Credential.RpId,
                "TODO: Generate Signature",
                _context.PasskeyCredentialRequest?.ClientDataHash,
                "TODO: Generate Authenticator Data",
                cipherView.Login.MainFido2Credential.CredentialId
            ));
        }

        public void CompleteAssertionRequest(ASPasskeyAssertionCredential assertionCredential)
        {
            if (_context == null)
            {
                ServiceContainer.Reset();
                CancelRequest(ASExtensionErrorCode.UserCanceled);
                return;
            }

            NSRunLoop.Main.BeginInvokeOnMainThread(() =>
            {
                ServiceContainer.Reset();
                ASExtensionContext?.CompleteAssertionRequest(assertionCredential, null);
            });
        }

        private bool CanProvideCredentialOnPasskeyRequest(CipherView cipherView)
        {
            return _context.PasskeyCredentialRequest != null && !cipherView.Login.HasFido2Credentials;
        }
    }
}

