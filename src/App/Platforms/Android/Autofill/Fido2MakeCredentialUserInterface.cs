using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;

namespace Bit.App.Platforms.Android.Autofill
{
    public class Fido2MakeCredentialUserInterface : IFido2MakeCredentialConfirmationUserInterface
    {
        private readonly IStateService _stateService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ICipherService _cipherService;
        private readonly IUserVerificationMediatorService _userVerificationMediatorService;
        private readonly IDeviceActionService _deviceActionService;

        private TaskCompletionSource<(string cipherId, bool? userVerified)> _confirmCredentialTcs;
        Fido2UserVerificationOptions? _currentDefaultUserVerificationOptions;

        public Fido2MakeCredentialUserInterface(IStateService stateService,
            IVaultTimeoutService vaultTimeoutService,
            ICipherService cipherService,
            IUserVerificationMediatorService userVerificationMediatorService,
            IDeviceActionService deviceActionService)
        {
            _stateService = stateService;
            _vaultTimeoutService = vaultTimeoutService;
            _cipherService = cipherService;
            _userVerificationMediatorService = userVerificationMediatorService;
            _deviceActionService = deviceActionService;
        }

        public bool HasVaultBeenUnlockedInThisTransaction => true;

        public async Task<(string CipherId, bool UserVerified)> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        {
            _confirmCredentialTcs?.TrySetCanceled();
            _confirmCredentialTcs = null;
            _confirmCredentialTcs = new TaskCompletionSource<(string cipherId, bool? userVerified)>();

            _currentDefaultUserVerificationOptions = new Fido2UserVerificationOptions(false, confirmNewCredentialParams.UserVerificationPreference, true, confirmNewCredentialParams.RpId);

            var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            messagingService?.Send("fidoNavigateToAutofillCipher", confirmNewCredentialParams);
            var (cipherId, isUserVerified) = await _confirmCredentialTcs.Task;

            var verified = isUserVerified ?? await VerifyUserAsync(cipherId, confirmNewCredentialParams.UserVerificationPreference, confirmNewCredentialParams.RpId);

            if (cipherId is null)
            {
                return await CreateNewLoginForFido2CredentialAsync(confirmNewCredentialParams, verified);
            }

            return (cipherId, verified);
        }

        private async Task<(string CipherId, bool UserVerified)> CreateNewLoginForFido2CredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams, bool userVerified)
        {
            if (!userVerified && await _userVerificationMediatorService.ShouldEnforceFido2RequiredUserVerificationAsync(new Fido2UserVerificationOptions
                (
                    false,
                    confirmNewCredentialParams.UserVerificationPreference,
                    true,
                    confirmNewCredentialParams.RpId
                )))
            {
                return (null, false);
            }

            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Loading);

                var cipherId = await _cipherService.CreateNewLoginForPasskeyAsync(confirmNewCredentialParams);

                await _deviceActionService.HideLoadingAsync();

                return (cipherId, userVerified);
            }
            catch
            {
                await _deviceActionService.HideLoadingAsync();
                throw;
            }
        }

        public async Task EnsureUnlockedVaultAsync()
        {
            if (!await _stateService.IsAuthenticatedAsync() || await _vaultTimeoutService.IsLockedAsync())
            {
                // this should never happen but just in case.
                throw new InvalidOperationException("Not authed or vault locked");
            }
        }

        public Task InformExcludedCredentialAsync(string[] existingCipherIds)
        {
            // TODO: Show excluded credential to the user in some screen.
            return Task.FromResult(true);
        }

        public void Confirm(string cipherId, bool? userVerified) => _confirmCredentialTcs?.TrySetResult((cipherId, userVerified));

        public void Cancel() => _confirmCredentialTcs?.TrySetCanceled();

        public void OnConfirmationException(Exception ex) => _confirmCredentialTcs?.TrySetException(ex);

        private async Task<bool> VerifyUserAsync(string selectedCipherId, Fido2UserVerificationPreference userVerificationPreference, string rpId)
        {
            try
            {
                if (selectedCipherId is null && userVerificationPreference == Fido2UserVerificationPreference.Discouraged)
                {
                    return false;
                }

                var shouldCheckMasterPasswordReprompt = false;
                if (selectedCipherId != null)
                {
                    var encrypted = await _cipherService.GetAsync(selectedCipherId);
                    var cipher = await encrypted.DecryptAsync();
                    shouldCheckMasterPasswordReprompt = cipher?.Reprompt == Core.Enums.CipherRepromptType.Password;
                }

                return await _userVerificationMediatorService.VerifyUserForFido2Async(
                    new Fido2UserVerificationOptions(
                        shouldCheckMasterPasswordReprompt,
                        userVerificationPreference,
                        true,
                        rpId)
                    );
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return false;
            }
        }

        public Fido2UserVerificationOptions? GetCurrentUserVerificationOptions() => _currentDefaultUserVerificationOptions;
    }
}
