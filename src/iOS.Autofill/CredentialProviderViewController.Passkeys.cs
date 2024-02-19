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

        public override async void PrepareInterfaceForPasskeyRegistration(IASCredentialRequest registrationRequest)
        {
            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                return;
            }

            ClipLogger.Log($"PIFPR(IASC)");

            try
            {
                switch (registrationRequest?.Type)
                {
                    case ASCredentialRequestType.PasskeyAssertion:
                        ClipLogger.Log($"PIFPR(IASC) -> Passkey");
                        var passkeyRegistrationRequest = Runtime.GetNSObject<ASPasskeyCredentialRequest>(registrationRequest.GetHandle());
                        await PrepareInterfaceForPasskeyRegistrationAsync(passkeyRegistrationRequest);
                        break;
                    default:
                        ClipLogger.Log($"PIFPR(IASC) -> Type not PA");
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
                ClipLogger.Log($"PIFPR Not iOS 17 or null passkey request/identity");
                return;
            }

            InitAppIfNeeded();

            if (!await IsAuthed())
            {
                ClipLogger.Log($"PIFPR Not Authed");
                await _accountsManager.NavigateOnAccountChangeAsync(false);
                return;
            }

            _context.PasskeyCredentialRequest = passkeyRegistrationRequest;
            _context.IsCreatingPasskey = true;

            var credIdentity = Runtime.GetNSObject<ASPasskeyCredentialIdentity>(passkeyRegistrationRequest.CredentialIdentity.GetHandle());

            _context.UrlString = credIdentity?.RelyingPartyIdentifier;

            ClipLogger.Log($"PIFPR MakeCredentialAsync");
            ClipLogger.Log($"PIFPR MakeCredentialAsync RpID: {credIdentity.RelyingPartyIdentifier}");
            ClipLogger.Log($"PIFPR MakeCredentialAsync UserName: {credIdentity.UserName}");
            ClipLogger.Log($"PIFPR MakeCredentialAsync UVP: {passkeyRegistrationRequest.UserVerificationPreference}");
            ClipLogger.Log($"PIFPR MakeCredentialAsync SA: {passkeyRegistrationRequest.SupportedAlgorithms?.Select(a => (int)a)}");
            ClipLogger.Log($"PIFPR MakeCredentialAsync UH: {credIdentity.UserHandle.GetBase64EncodedString(NSDataBase64EncodingOptions.None)}");

            var result = await Fido2AuthService.MakeCredentialAsync(new Bit.Core.Utilities.Fido2.Fido2AuthenticatorMakeCredentialParams
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
            });

            await ASHelpers.ReplaceAllIdentitiesAsync();

            ClipLogger.Log($"PIFPR Completing");
            ClipLogger.Log($"PIFPR Completing - RpId: {credIdentity.RelyingPartyIdentifier}");
            ClipLogger.Log($"PIFPR Completing - CDH: {passkeyRegistrationRequest.ClientDataHash.GetBase64EncodedString(NSDataBase64EncodingOptions.None)}");
            ClipLogger.Log($"PIFPR Completing - CID: {Convert.ToBase64String(result.CredentialId, Base64FormattingOptions.None)}");
            ClipLogger.Log($"PIFPR Completing - AO: {Convert.ToBase64String(result.AttestationObject, Base64FormattingOptions.None)}");

            var expired = await ExtensionContext.CompleteRegistrationRequestAsync(new ASPasskeyRegistrationCredential(
                            credIdentity.RelyingPartyIdentifier,
                            passkeyRegistrationRequest.ClientDataHash,
                            NSData.FromArray(result.CredentialId),
                            NSData.FromArray(result.AttestationObject)));

            ClipLogger.Log($"CompleteRegistrationRequestAsync: {expired}");
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
                ClipLogger.Log($"ClientDataHash: {_context.PasskeyCredentialRequest.ClientDataHash}");
                ClipLogger.Log($"ClientDataHash BA: {_context.PasskeyCredentialRequest.ClientDataHash.ToByteArray()}");
                ClipLogger.Log($"ClientDataHash base64: {_context.PasskeyCredentialRequest.ClientDataHash.GetBase64EncodedString(NSDataBase64EncodingOptions.None)}");
                ClipLogger.Log($"ClientDataHash base64 from bytes: {Convert.ToBase64String(_context.PasskeyCredentialRequest.ClientDataHash.ToByteArray(), Base64FormattingOptions.None)}");

                var fido2AssertionResult = await Fido2AuthService.GetAssertionAsync(new Bit.Core.Utilities.Fido2.Fido2AuthenticatorGetAssertionParams
                {
                    RpId = rpId,
                    Hash = _context.PasskeyCredentialRequest.ClientDataHash.ToArray(),
                    RequireUserVerification = _context.PasskeyCredentialRequest.UserVerificationPreference == "required",
                    RequireUserPresence = false,
                    AllowCredentialDescriptorList = new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor[]
                    {
                        new Bit.Core.Utilities.Fido2.PublicKeyCredentialDescriptor
                        {
                            Id = credentialIdData.ToArray()
                        }
                    }
                });


                ClipLogger.Log("fido2AssertionResult:" + fido2AssertionResult);

                var selectedUserHandleData = fido2AssertionResult.SelectedCredential != null
                    ? NSData.FromArray(fido2AssertionResult.SelectedCredential.UserHandle)
                    : (NSData)userHandleData;


                ClipLogger.Log("selectedUserHandleData:" + selectedUserHandleData);

                var selectedCredentialIdData = fido2AssertionResult.SelectedCredential != null
                    ? NSData.FromArray(fido2AssertionResult.SelectedCredential.Id)
                    : credentialIdData;

                ClipLogger.Log("selectedCredentialIdData:" + selectedCredentialIdData);

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
                ClipLogger.Log("CompleteAssertionRequestAsync -> InvalidOperationException NoOp");
                return;
            }
        }

        public async Task CompleteAssertionRequest(ASPasskeyAssertionCredential assertionCredential)
        {
            try
            {
                ClipLogger.Log("CompleteAssertionRequest(ASPasskeyAssertionCredential assertionCredential");
                if (assertionCredential is null)
                {
                    ClipLogger.Log("CompleteAssertionRequest(ASPasskeyAssertionCredential assertionCredential -> assertionCredential is null");
                    ServiceContainer.Reset();
                    CancelRequest(ASExtensionErrorCode.UserCanceled);
                    return;
                }

            //NSRunLoop.Main.BeginInvokeOnMainThread(() =>
            //{
                ServiceContainer.Reset();
#pragma warning disable CA1416 // Validate platform compatibility
                ClipLogger.Log("CompleteAssertionRequest(ASPasskeyAssertionCredential assertionCredential -> completing");
            var expired = await ExtensionContext.CompleteAssertionRequestAsync(assertionCredential);
                //ExtensionContext.CompleteAssertionRequest(assertionCredential, expired =>
                //{
                //    ClipLogger.Log($"ASExtensionContext?.CompleteAssertionRequest: {expired}");
                //});

                ClipLogger.Log($"CompleteAssertionRequest(ASPasskeyAssertionCredential assertionCredential -> Completed {expired}");
#pragma warning restore CA1416 // Validate platform compatibility
                //});

            }
            catch (Exception ex)
            {
                ClipLogger.Log($"CompleteAssertionRequest(ASPasskeyAssertionCredential assertionCredential -> failed {ex}");
            }
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
            // iOS doesn't seem to provide the ExcludeCredentialDescriptorList so nothing to do here currently.
            return Task.CompletedTask;
        }

        public async Task<Fido2ConfirmNewCredentialResult> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        {
            ClipLogger.Log($"ConfirmNewCredentialAsync");
            _context.ConfirmNewCredentialTcs?.SetCanceled();
            _context.ConfirmNewCredentialTcs = new TaskCompletionSource<Fido2ConfirmNewCredentialResult>();
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

            return await _context.ConfirmNewCredentialTcs.Task;
        }

        public async Task EnsureUnlockedVaultAsync()
        {
            if (_context.IsCreatingPasskey)
            {
                ClipLogger.Log($"EnsureUnlockedVaultAsync creating passkey");
                if (!await IsLocked())
                {
                    ClipLogger.Log($"EnsureUnlockedVaultAsync not locked");
                    return;
                }

                _context.UnlockVaultTcs?.SetCanceled();
                _context.UnlockVaultTcs = new TaskCompletionSource<bool>();
                MainThread.BeginInvokeOnMainThread(() =>
                {
                    try
                    {
                        ClipLogger.Log($"EnsureUnlockedVaultAsync performing lock segue");
                        PerformSegue("lockPasswordSegue", this);
                    }
                    catch (Exception ex)
                    {
                        ClipLogger.Log($"EnsureUnlockedVaultAsync {ex}");
                    }
                });

                ClipLogger.Log($"EnsureUnlockedVaultAsync awaiting for unlock");
                await _context.UnlockVaultTcs.Task;
                return;
            }

            ClipLogger.Log($"EnsureUnlockedVaultAsync Passkey selection");
            if (!await IsAuthed() || await IsLocked())
            {
                CancelRequest(ASExtensionErrorCode.UserInteractionRequired);
                throw new InvalidOperationException("Not authed or locked");
            }
        }
    }
}

