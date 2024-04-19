using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities.Fido2;

namespace Bit.App.Platforms.Android.Autofill
{
    public interface IFido2AndroidGetAssertionUserInterface : IFido2GetAssertionUserInterface
    {
        void Init(string cipherId,
            bool userVerified,
            Func<bool> hasVaultBeenUnlockedInThisTransaction,
            string rpId);
    }

    public class Fido2GetAssertionUserInterface : Core.Utilities.Fido2.Fido2GetAssertionUserInterface, IFido2AndroidGetAssertionUserInterface
    {
        private readonly IStateService _stateService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ICipherService _cipherService;
        private readonly IUserVerificationMediatorService _userVerificationMediatorService;

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
                // this should never happen but just in case.
                throw new InvalidOperationException("Not authed or vault locked");
            }
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
