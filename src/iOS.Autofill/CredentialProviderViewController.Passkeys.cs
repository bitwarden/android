using System;
using System.Linq;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.iOS.Core.Utilities;
using Foundation;
using Microsoft.Maui.ApplicationModel;
using ObjCRuntime;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController, IAccountsManagerHost
    {
        private readonly LazyResolve<IFido2AuthenticatorService> _fido2AuthService = new LazyResolve<IFido2AuthenticatorService>();

        public override async void PrepareInterfaceForPasskeyRegistration(IASCredentialRequest registrationRequest)
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                return;
            }

            try
            {
                switch (registrationRequest?.Type)
                {
                    case ASCredentialRequestType.PasskeyAssertion:
                        var passkeyRegistrationRequest = Runtime.GetNSObject<ASPasskeyCredentialRequest>(registrationRequest.GetHandle());
                        await PrepareInterfaceForPasskeyRegistrationAsync(passkeyRegistrationRequest);
                        break;
                    default:
                        CancelRequest(ASExtensionErrorCode.Failed);
                        break;
                }

            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }

        private async Task PrepareInterfaceForPasskeyRegistrationAsync(ASPasskeyCredentialRequest passkeyRegistrationRequest)
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0) || passkeyRegistrationRequest?.CredentialIdentity is null)
            {
                return;
            }

            InitAppIfNeeded();

            if (!await IsAuthed())
            {
                await _accountsManager.NavigateOnAccountChangeAsync(false);
                return;
            }

            _context.PasskeyCredentialRequest = passkeyRegistrationRequest;
            _context.IsCreatingPasskey = true;

            var credIdentity = Runtime.GetNSObject<ASPasskeyCredentialIdentity>(passkeyRegistrationRequest.CredentialIdentity.GetHandle());

            _context.UrlString = credIdentity?.RelyingPartyIdentifier;

            var result = await _fido2AuthService.Value.MakeCredentialAsync(new Bit.Core.Utilities.Fido2.Fido2AuthenticatorMakeCredentialParams
            {
                Hash = passkeyRegistrationRequest.ClientDataHash.ToArray(),
                CredTypesAndPubKeyAlgs = GetCredTypesAndPubKeyAlgs(passkeyRegistrationRequest.SupportedAlgorithms),
                RequireUserVerification = passkeyRegistrationRequest.UserVerificationPreference == "required",
                RequireResidentKey = true,
                RpEntity = new PublicKeyCredentialRpEntity
                {
                    Id = credIdentity.RelyingPartyIdentifier,
                    Name = credIdentity.RelyingPartyIdentifier
                },
                UserEntity = new PublicKeyCredentialUserEntity
                {
                    Id = credIdentity.UserHandle.ToArray(),
                    Name = credIdentity.UserName,
                    DisplayName = credIdentity.UserName
                }
            }, new Fido2MakeCredentialUserInterface(EnsureUnlockedVaultAsync, _context, OnConfirmingNewCredential));

            await ASHelpers.ReplaceAllIdentitiesAsync();

            var expired = await ExtensionContext.CompleteRegistrationRequestAsync(new ASPasskeyRegistrationCredential(
                            credIdentity.RelyingPartyIdentifier,
                            passkeyRegistrationRequest.ClientDataHash,
                            NSData.FromArray(result.CredentialId),
                            NSData.FromArray(result.AttestationObject)));
        }

        private PublicKeyCredentialParameters[] GetCredTypesAndPubKeyAlgs(NSNumber[] supportedAlgorithms)
        {
            if (supportedAlgorithms?.Any() != true)
            {
                return new PublicKeyCredentialParameters[]
                {
                    new PublicKeyCredentialParameters
                    {
                        Type = Bit.Core.Constants.DefaultFido2CredentialType,
                        Alg = (int)Fido2AlgorithmIdentifier.ES256
                    },
                    new PublicKeyCredentialParameters
                    {
                        Type = Bit.Core.Constants.DefaultFido2CredentialType,
                        Alg = (int)Fido2AlgorithmIdentifier.RS256
                    }
                };
            }

            return supportedAlgorithms
                .Where(alg => (int)alg == (int)Fido2AlgorithmIdentifier.ES256)
                .Select(alg => new PublicKeyCredentialParameters
                {
                    Type = Bit.Core.Constants.DefaultFido2CredentialType,
                    Alg = (int)alg
                }).ToArray();
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
                // TODO: Add user verification and remove hardcoding on the user interface "userVerified"
                var fido2AssertionResult = await _fido2AuthService.Value.GetAssertionAsync(new Bit.Core.Utilities.Fido2.Fido2AuthenticatorGetAssertionParams
                {
                    RpId = rpId,
                    Hash = _context.PasskeyCredentialRequest.ClientDataHash.ToArray(),
                    RequireUserVerification = _context.PasskeyCredentialRequest.UserVerificationPreference == "required",
                    AllowCredentialDescriptorList = new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor[]
                    {
                        new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor
                        {
                            Id = credentialIdData.ToArray()
                        }
                    }
                }, new Fido2GetAssertionUserInterface(cipherId, true, EnsureUnlockedVaultAsync, () => Task.FromResult(true)));

                var selectedUserHandleData = fido2AssertionResult.SelectedCredential != null
                    ? NSData.FromArray(fido2AssertionResult.SelectedCredential.UserHandle)
                    : (NSData)userHandleData;

                var selectedCredentialIdData = fido2AssertionResult.SelectedCredential != null
                    ? NSData.FromArray(fido2AssertionResult.SelectedCredential.Id)
                    : credentialIdData;

                await CompleteAssertionRequest(new ASPasskeyAssertionCredential(
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

        public async Task CompleteAssertionRequest(ASPasskeyAssertionCredential assertionCredential)
        {
            try
            {
                if (assertionCredential is null)
                {
                    ServiceContainer.Reset();
                    CancelRequest(ASExtensionErrorCode.UserCanceled);
                    return;
                }

                ServiceContainer.Reset();
#pragma warning disable CA1416 // Validate platform compatibility
            var expired = await ExtensionContext.CompleteAssertionRequestAsync(assertionCredential);
#pragma warning restore CA1416 // Validate platform compatibility

            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        private bool CanProvideCredentialOnPasskeyRequest(CipherView cipherView)
        {
            return _context.PasskeyCredentialRequest != null && !cipherView.Login.HasFido2Credentials;
        }

        private void OnConfirmingNewCredential()
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                try
                {
                    PerformSegue(SegueConstants.LOGIN_LIST, this);
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }
            });
        }

        private async Task EnsureUnlockedVaultAsync()
        {
            if (_context.IsCreatingPasskey)
            {
                if (!await IsLocked())
                {
                    return;
                }

                _context.UnlockVaultTcs?.SetCanceled();
                _context.UnlockVaultTcs = new TaskCompletionSource<bool>();
                MainThread.BeginInvokeOnMainThread(() =>
                {
                    try
                    {
                        PerformSegue(SegueConstants.LOCK, this);
                    }
                    catch (Exception ex)
                    {
                        LoggerHelper.LogEvenIfCantBeResolved(ex);
                    }
                });

                await _context.UnlockVaultTcs.Task;
                return;
            }

            if (!await IsAuthed() || await IsLocked())
            {
                CancelRequest(ASExtensionErrorCode.UserInteractionRequired);
                throw new InvalidOperationException("Not authed or locked");
            }
        }
    }
}
