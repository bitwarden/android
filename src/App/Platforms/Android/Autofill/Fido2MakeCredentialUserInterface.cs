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
        private readonly IPlatformUtilsService _platformUtilsService;
        private LazyResolve<IMessagingService> _messagingService = new LazyResolve<IMessagingService>();

        private TaskCompletionSource<(string cipherId, bool? userVerified)> _confirmCredentialTcs;
        private TaskCompletionSource<bool> _unlockVaultTcs;
        private Fido2UserVerificationOptions? _currentDefaultUserVerificationOptions;
        private Func<bool> _checkHasVaultBeenUnlockedInThisTransaction;

        public Fido2MakeCredentialUserInterface(IStateService stateService,
            IVaultTimeoutService vaultTimeoutService,
            ICipherService cipherService,
            IUserVerificationMediatorService userVerificationMediatorService,
            IDeviceActionService deviceActionService,
            IPlatformUtilsService platformUtilsService)
        {
            _stateService = stateService;
            _vaultTimeoutService = vaultTimeoutService;
            _cipherService = cipherService;
            _userVerificationMediatorService = userVerificationMediatorService;
            _deviceActionService = deviceActionService;
            _platformUtilsService = platformUtilsService;
        }

        public bool HasVaultBeenUnlockedInThisTransaction => _checkHasVaultBeenUnlockedInThisTransaction?.Invoke() == true;

        public bool IsConfirmingNewCredential => _confirmCredentialTcs?.Task != null && !_confirmCredentialTcs.Task.IsCompleted;
        public bool IsWaitingUnlockVault => _unlockVaultTcs?.Task != null && !_unlockVaultTcs.Task.IsCompleted;

        public async Task<(string CipherId, bool UserVerified)> ConfirmNewCredentialAsync(Fido2ConfirmNewCredentialParams confirmNewCredentialParams)
        {
            _confirmCredentialTcs?.TrySetCanceled();
            _confirmCredentialTcs = null;
            _confirmCredentialTcs = new TaskCompletionSource<(string cipherId, bool? userVerified)>();

            _currentDefaultUserVerificationOptions = new Fido2UserVerificationOptions(false, confirmNewCredentialParams.UserVerificationPreference, HasVaultBeenUnlockedInThisTransaction, confirmNewCredentialParams.RpId);
            
            _messagingService.Value.Send(Bit.Core.Constants.CredentialNavigateToAutofillCipherMessageCommand, confirmNewCredentialParams);

            var (cipherId, isUserVerified) = await _confirmCredentialTcs.Task;

            var verified = isUserVerified;
            if (verified is null)
            {
                var userVerification = await VerifyUserAsync(cipherId, confirmNewCredentialParams.UserVerificationPreference, confirmNewCredentialParams.RpId);
                // TODO: If cancelled then let the user choose another cipher.
                // I think this can be done by showing a message to the uesr and recursive calling of this method ConfirmNewCredentialAsync
                verified = !userVerification.IsCancelled && userVerification.Result;
            }

            if (cipherId is null)
            {
                return await CreateNewLoginForFido2CredentialAsync(confirmNewCredentialParams, verified.Value);
            }

            return (cipherId, verified.Value);
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
            if (!await _stateService.IsAuthenticatedAsync()
                ||
                await _vaultTimeoutService.IsLoggedOutByTimeoutAsync()
                ||
                await _vaultTimeoutService.ShouldLogOutByTimeoutAsync())
            {
                await NavigateAndWaitForUnlockAsync(Bit.Core.Enums.NavigationTarget.HomeLogin);
                return;
            }

            if (!await _vaultTimeoutService.IsLockedAsync())
            {
                return;
            }

            await NavigateAndWaitForUnlockAsync(Bit.Core.Enums.NavigationTarget.Lock);
        }

        private async Task NavigateAndWaitForUnlockAsync(Bit.Core.Enums.NavigationTarget navTarget)
        {
            _unlockVaultTcs?.TrySetCanceled();
            _unlockVaultTcs = new TaskCompletionSource<bool>();

            _messagingService.Value.Send(Bit.Core.Constants.NavigateToMessageCommand, navTarget);

            await _unlockVaultTcs.Task;
        }

        public Task InformExcludedCredentialAsync(string[] existingCipherIds)
        {
            // TODO: Show excluded credential to the user in some screen.
            return Task.FromResult(true);
        }

        public void SetCheckHasVaultBeenUnlockedInThisTransaction(Func<bool> checkHasVaultBeenUnlockedInThisTransaction)
        {
            _checkHasVaultBeenUnlockedInThisTransaction = checkHasVaultBeenUnlockedInThisTransaction;
        }

        public void Confirm(string cipherId, bool? userVerified) => _confirmCredentialTcs?.TrySetResult((cipherId, userVerified));
        public void ConfirmVaultUnlocked() => _unlockVaultTcs?.TrySetResult(true);

        public async Task ConfirmAsync(string cipherId, bool alreadyHasFido2Credential, bool? userVerified)
        {
            if (alreadyHasFido2Credential
                &&
                !await _platformUtilsService.ShowDialogAsync(
                    AppResources.ThisItemAlreadyContainsAPasskeyAreYouSureYouWantToOverwriteTheCurrentPasskey,
                    AppResources.OverwritePasskey,
                    AppResources.Yes,
                    AppResources.No))
            {
                return;
            }

            Confirm(cipherId, userVerified);
        }

        public void Cancel() => _confirmCredentialTcs?.TrySetCanceled();

        public void OnConfirmationException(Exception ex) => _confirmCredentialTcs?.TrySetException(ex);

        private async Task<CancellableResult<bool>> VerifyUserAsync(string selectedCipherId, Fido2UserVerificationPreference userVerificationPreference, string rpId)
        {
            try
            {
                if (selectedCipherId is null && userVerificationPreference == Fido2UserVerificationPreference.Discouraged)
                {
                    return new CancellableResult<bool>(false);
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
                        HasVaultBeenUnlockedInThisTransaction,
                        rpId)
                    );
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return new CancellableResult<bool>(false);
            }
        }

        public Fido2UserVerificationOptions? GetCurrentUserVerificationOptions() => _currentDefaultUserVerificationOptions;
    }
}
