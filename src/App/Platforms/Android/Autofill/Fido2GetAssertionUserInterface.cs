using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities.Fido2;
using Bit.Droid.Autofill;

namespace Bit.App.Platforms.Android.Autofill
{
    public interface IFido2AndroidGetAssertionUserInterface : IFido2GetAssertionUserInterface
    {
        void Init(string cipherId,
            bool userVerified,
            Func<bool> hasVaultBeenUnlockedInThisTransaction,
            string rpId);

        /// <summary>
        /// Call this after the vault was unlocked so that Fido2 credential autofill can proceed.
        /// </summary>
        void ConfirmVaultUnlocked(bool unlocked);
    }

    public class Fido2GetAssertionUserInterface : Core.Utilities.Fido2.Fido2GetAssertionUserInterface, IFido2AndroidGetAssertionUserInterface
    {
        private readonly IStateService _stateService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ICipherService _cipherService;
        private readonly IUserVerificationMediatorService _userVerificationMediatorService;

        private TaskCompletionSource<bool> _unlockVaultTcs;

        public Fido2GetAssertionUserInterface(IStateService stateService, 
            IVaultTimeoutService vaultTimeoutService,
            ICipherService cipherService,
            IUserVerificationMediatorService userVerificationMediatorService)
        {
            _stateService = stateService;
            _vaultTimeoutService = vaultTimeoutService;
            _cipherService = cipherService;
            _userVerificationMediatorService = userVerificationMediatorService;
        }

        public void Init(string cipherId,
            bool userVerified,
            Func<bool> hasVaultBeenUnlockedInThisTransaction,
            string rpId)
        {
            Init(cipherId, 
                userVerified, 
                EnsureAuthenAndVaultUnlockedAsync, 
                hasVaultBeenUnlockedInThisTransaction, 
                (cipherId, userVerificationPreference) => VerifyUserAsync(cipherId, userVerificationPreference, rpId, hasVaultBeenUnlockedInThisTransaction()));
        }

        public async Task EnsureAuthenAndVaultUnlockedAsync()
        {
            if (!await _stateService.IsAuthenticatedAsync() || await _vaultTimeoutService.IsLockedAsync())
            {
                if (await _stateService.GetVaultTimeoutAsync() != 0)
                {
                    // this should never happen but just in case.
                    throw new InvalidOperationException("Not authed or vault locked");
                }

                // if vault timeout is immediate, then we need to unlock the vault
                if (!await NavigateAndWaitForUnlockAsync())
                {
                    throw new InvalidOperationException("Couldn't unlock with immediate timeout");
                }
            }
        }

        public void ConfirmVaultUnlocked(bool unlocked) => _unlockVaultTcs?.TrySetResult(unlocked);

        private async Task<bool> NavigateAndWaitForUnlockAsync()
        {
            var credentialProviderSelectionActivity = Platform.CurrentActivity as CredentialProviderSelectionActivity;
            if (credentialProviderSelectionActivity == null)
            {
                throw new InvalidOperationException("Can't get current activity");
            }

            _unlockVaultTcs?.TrySetCanceled();
            _unlockVaultTcs = new TaskCompletionSource<bool>();

            credentialProviderSelectionActivity.LaunchToUnlock();

            return await _unlockVaultTcs.Task;
        }

        private async Task<bool> VerifyUserAsync(string selectedCipherId, Fido2UserVerificationPreference userVerificationPreference, string rpId, bool vaultUnlockedDuringThisTransaction)
        {
            try
            {
                var encrypted = await _cipherService.GetAsync(selectedCipherId);
                var cipher = await encrypted.DecryptAsync();

                var userVerification = await _userVerificationMediatorService.VerifyUserForFido2Async(
                    new Fido2UserVerificationOptions(
                        cipher?.Reprompt == Core.Enums.CipherRepromptType.Password,
                        userVerificationPreference,
                        vaultUnlockedDuringThisTransaction,
                        rpId)
                    );
                return !userVerification.IsCancelled && userVerification.Result;
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return false;
            }
        }
    }
}
