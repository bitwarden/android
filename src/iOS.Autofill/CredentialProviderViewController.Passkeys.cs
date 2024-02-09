using System;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Bit.iOS.Core.Utilities;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController, IAccountsManagerHost, IFido2UserInterface
    {
        private IFido2AuthenticatorService _fido2AuthService;
        private IFido2AuthenticatorService Fido2AuthService
        {
            get
            {
                if (_fido2AuthService is null)
                {
                    _fido2AuthService = ServiceContainer.Resolve<IFido2AuthenticatorService>();
                    _fido2AuthService.Init(this);
                }
                return _fido2AuthService;
            }
        }

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

        public async Task CompleteAssertionRequestAsync(string rpId, NSData userHandleData, NSData credentialIdData, string cipherId)
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                OnProvidingCredentialException(new InvalidOperationException("Trying to complete assertion request before iOS 17"));
                return;
            }

            if (_context.PasskeyCredentialRequest is null)
            {
                OnProvidingCredentialException(new InvalidOperationException("Trying to complete assertion request without a PasskeyCredentialRequest"));
                return;
            }

            try
            {
                var fido2AssertionResult = await Fido2AuthService.GetAssertionAsync(new Bit.Core.Utilities.Fido2.Fido2AuthenticatorGetAssertionParams
                {
                    RpId = rpId,
                    ClientDataHash = _context.PasskeyCredentialRequest.ClientDataHash.ToByteArray(),
                    RequireUserVerification = _context.PasskeyCredentialRequest.UserVerificationPreference == "required",
                    RequireUserPresence = false,
                    AllowCredentialDescriptorList = new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor[]
                    {
                        new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor { Id = credentialIdData.ToByteArray() }
                    }
                });

                var selectedUserHandleData = fido2AssertionResult.SelectedCredential != null
                    ? NSData.FromArray(fido2AssertionResult.SelectedCredential.UserHandle)
                    : (NSData)userHandleData;

                var selectedCredentialIdData = fido2AssertionResult.SelectedCredential != null
                    ? new Guid(fido2AssertionResult.SelectedCredential.Id).ToString()
                    : credentialIdData;

                CompleteAssertionRequest(new ASPasskeyAssertionCredential(
                    selectedUserHandleData,
                    rpId,
                    NSData.FromArray(fido2AssertionResult.Signature),
                    _context.PasskeyCredentialRequest.ClientDataHash,
                    NSData.FromArray(fido2AssertionResult.AuthenticatorData),
                    selectedCredentialIdData
                ));
            }
            catch (InvalidOperationException)
            {
                return;
            }
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

        // IFido2UserInterface

        public Task<Fido2PickCredentialResult> PickCredentialAsync(Fido2PickCredentialParams pickCredentialParams)
        {
            return Task.FromResult(new Fido2PickCredentialResult());
        }

        public Task InformExcludedCredential(string[] existingCipherIds)
        {
            return Task.CompletedTask;
        }

        public Task<Fido2ConfirmNewCredentialResult> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        {
            return Task.FromResult(new Fido2ConfirmNewCredentialResult());
        }

        public async Task EnsureUnlockedVaultAsync()
        {
            if (!await IsAuthed() || await IsLocked())
            {
                CancelRequest(ASExtensionErrorCode.UserInteractionRequired);
                throw new InvalidOperationException("Not authed or locked");
            }
        }
    }
}

