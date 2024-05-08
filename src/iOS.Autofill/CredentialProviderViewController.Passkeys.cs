using System;
using System.Linq;
using System.Threading.Tasks;
using AuthenticationServices;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.iOS.Autofill.Utilities;
using Bit.iOS.Core.Utilities;
using Foundation;
using Microsoft.Maui.ApplicationModel;
using ObjCRuntime;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class CredentialProviderViewController : ASCredentialProviderViewController, IAccountsManagerHost
    {
        private readonly LazyResolve<IFido2MediatorService> _fido2MediatorService = new LazyResolve<IFido2MediatorService>();
        private readonly LazyResolve<IPlatformUtilsService> _platformUtilsService = new LazyResolve<IPlatformUtilsService>();
        private readonly LazyResolve<IUserVerificationMediatorService> _userVerificationMediatorService = new LazyResolve<IUserVerificationMediatorService>();
        private readonly LazyResolve<ICipherService> _cipherService = new LazyResolve<ICipherService>();

        [Export("prepareCredentialListForServiceIdentifiers:requestParameters:")]
        public override void PrepareCredentialList(ASCredentialServiceIdentifier[] serviceIdentifiers, ASPasskeyCredentialRequestParameters requestParameters)
        {
            try
            {
                if (UIDevice.CurrentDevice.CheckSystemVersion(17, 0) && !string.IsNullOrEmpty(requestParameters?.RelyingPartyIdentifier))
                {
                    _context.PasskeyCredentialRequestParameters = requestParameters;
                }

                PrepareCredentialList(serviceIdentifiers);
            }
            catch (Exception ex)
            {
                OnProvidingCredentialException(ex);
            }
        }

        public override async void PrepareInterfaceForPasskeyRegistration(IASCredentialRequest registrationRequest)
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                return;
            }

            _context.VaultUnlockedDuringThisSession = false;

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
            catch (Fido2AuthenticatorException)
            {
                CancelRequest(ASExtensionErrorCode.Failed);
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

            await InitAppIfNeededAsync();

            if (!await IsAuthed())
            {
                await _accountsManager.NavigateOnAccountChangeAsync(false);
                return;
            }

            _context.PasskeyCredentialRequest = passkeyRegistrationRequest;
            _context.IsCreatingPasskey = true;

            var credIdentity = Runtime.GetNSObject<ASPasskeyCredentialIdentity>(passkeyRegistrationRequest.CredentialIdentity.GetHandle());

            _context.UrlString = credIdentity?.RelyingPartyIdentifier;
            
            try
            {
                var result = await _fido2MediatorService.Value.MakeCredentialAsync(new Bit.Core.Utilities.Fido2.Fido2AuthenticatorMakeCredentialParams
                {
                    Hash = passkeyRegistrationRequest.ClientDataHash.ToArray(),
                    CredTypesAndPubKeyAlgs = GetCredTypesAndPubKeyAlgs(passkeyRegistrationRequest.SupportedAlgorithms),
                    UserVerificationPreference = Fido2UserVerificationPreferenceExtensions.ToFido2UserVerificationPreference(passkeyRegistrationRequest.UserVerificationPreference),
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
                }, new Fido2MakeCredentialUserInterface(EnsureUnlockedVaultAsync,
                    () => _context.VaultUnlockedDuringThisSession,
                    _context,
                    OnConfirmingNewCredential,
                    VerifyUserAsync));

                await ASHelpers.ReplaceAllIdentitiesAsync();

                var expired = await ExtensionContext.CompleteRegistrationRequestAsync(new ASPasskeyRegistrationCredential(
                                credIdentity.RelyingPartyIdentifier,
                                passkeyRegistrationRequest.ClientDataHash,
                                NSData.FromArray(result.CredentialId),
                                NSData.FromArray(result.AttestationObject)));
            }
            catch
            {
                try
                {
                    await _platformUtilsService.Value.ShowDialogAsync(
                        string.Format(AppResources.ThereWasAProblemCreatingAPasskeyForXTryAgainLater, credIdentity?.RelyingPartyIdentifier),
                        AppResources.ErrorCreatingPasskey);
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }

                throw;
            }
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
            await InitAppIfNeededAsync();
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
                var fido2AssertionResult = await _fido2MediatorService.Value.GetAssertionAsync(new Bit.Core.Utilities.Fido2.Fido2AuthenticatorGetAssertionParams
                {
                    RpId = rpId,
                    Hash = _context.PasskeyCredentialRequest.ClientDataHash.ToArray(),
                    UserVerificationPreference = Fido2UserVerificationPreferenceExtensions.ToFido2UserVerificationPreference(_context.PasskeyCredentialRequest.UserVerificationPreference),
                    AllowCredentialDescriptorList = new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor[]
                    {
                        new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor
                        {
                            Id = credentialIdData.ToArray()
                        }
                    }
                }, new Fido2GetAssertionUserInterface(cipherId, false,
                    EnsureUnlockedVaultAsync,
                    () => _context?.VaultUnlockedDuringThisSession ?? false,
                    VerifyUserAsync));

                if (fido2AssertionResult.SelectedCredential is null)
                {
                    throw new NullReferenceException("SelectedCredential must have a value");
                }

                await CompleteAssertionRequest(new ASPasskeyAssertionCredential(
                    NSData.FromArray(fido2AssertionResult.SelectedCredential.UserHandle),
                    rpId,
                    NSData.FromArray(fido2AssertionResult.Signature),
                    _context.PasskeyCredentialRequest.ClientDataHash,
                    NSData.FromArray(fido2AssertionResult.AuthenticatorData),
                    NSData.FromArray(fido2AssertionResult.SelectedCredential.Id)
                ));
            }
            catch (InvalidOperationNeedsUIException)
            {
                return;
            }
            catch
            {
                try
                {
                    if (_context?.IsExecutingWithoutUserInteraction == false)
                    {
                        await _platformUtilsService.Value.ShowDialogAsync(
                            string.Format(AppResources.ThereWasAProblemReadingAPasskeyForXTryAgainLater, rpId),
                            AppResources.ErrorReadingPasskey);
                    }
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }

                throw;
            }
        }

        internal async Task<bool> VerifyUserAsync(string selectedCipherId, Fido2UserVerificationPreference userVerificationPreference)
        {
            try
            {
                var encrypted = await _cipherService.Value.GetAsync(selectedCipherId);
                var cipher = await encrypted.DecryptAsync();

                var cResult = await _userVerificationMediatorService.Value.VerifyUserForFido2Async(
                    new Fido2UserVerificationOptions(
                        cipher?.Reprompt == Bit.Core.Enums.CipherRepromptType.Password,
                        userVerificationPreference,
                        _context.VaultUnlockedDuringThisSession,
                        _context.PasskeyCredentialIdentity?.RelyingPartyIdentifier,
                        async () =>
                        {
                            if (_context.IsExecutingWithoutUserInteraction)
                            {
                                CancelRequest(ASExtensionErrorCode.UserInteractionRequired);
                                throw new InvalidOperationNeedsUIException();
                            }

                            // HACK: [PM-6685] There are some devices that end up with a race condition when doing biometrics authentication
                            // that the check is trying to be done before the iOS extension UI is shown, which cause the bio check to fail.
                            // So a workaround is to show a toast which force the iOS extension UI to be shown and then awaiting for the
                            // precondition that the view did appear before continuing with the verification.
                            _platformUtilsService.Value.ShowToast(null, null, AppResources.VerifyingIdentityEllipsis);

                            await _conditionedAwaiterManager.Value.GetAwaiterForPrecondition(AwaiterPrecondition.AutofillIOSExtensionViewDidAppear);
                        }
                    )
                );
                return !cResult.IsCancelled && cResult.Result;
            }
            catch (InvalidOperationNeedsUIException)
            {
                throw;
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return false;
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

        private void OnConfirmingNewCredential()
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                try
                {
                    DismissViewController(false, () => PerformSegue(SegueConstants.LOGIN_LIST, this));
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
                if (!await IsAuthed()
                    ||
                    await _vaultTimeoutService.Value.IsLoggedOutByTimeoutAsync()
                    ||
                    await _vaultTimeoutService.Value.ShouldLogOutByTimeoutAsync())
                {
                    await NavigateAndWaitForUnlockAsync(Bit.Core.Enums.NavigationTarget.HomeLogin);
                    return;
                }

                if (!await IsLocked())
                {
                    return;
                }

                await NavigateAndWaitForUnlockAsync(Bit.Core.Enums.NavigationTarget.Lock);
                return;
            }

            if (!await IsAuthed() || await IsLocked())
            {
                CancelRequest(ASExtensionErrorCode.UserInteractionRequired);
                throw new InvalidOperationNeedsUIException("Not authed or locked");
            }
        }

        private async Task NavigateAndWaitForUnlockAsync(Bit.Core.Enums.NavigationTarget navTarget)
        {
            _context.UnlockVaultTcs?.TrySetCanceled();
            _context.UnlockVaultTcs = new TaskCompletionSource<bool>();
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                DoNavigate(navTarget);
            });

            await _context.UnlockVaultTcs.Task;
        }
    }
}
