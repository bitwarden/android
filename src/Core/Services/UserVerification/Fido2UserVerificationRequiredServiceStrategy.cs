using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Services.UserVerification
{
    public class Fido2UserVerificationRequiredServiceStrategy : IUserVerificationServiceStrategy
    {
        private readonly IUserVerificationMediatorService _userVerificationMediatorService;
        private readonly IPlatformUtilsService _platformUtilsService;

        public Fido2UserVerificationRequiredServiceStrategy(IUserVerificationMediatorService userVerificationMediatorService,
            IPlatformUtilsService platformUtilsService)
        {
            _userVerificationMediatorService = userVerificationMediatorService;
            _platformUtilsService = platformUtilsService;
        }

        public async Task<bool> VerifyUserForFido2Async(Fido2UserVerificationOptions options)
        {
            if (options.HasVaultBeenUnlockedInTransaction)
            {
                return true;
            }

            options.OnNeedUI?.Invoke();

            var (canPerformOSUnlock, isOSUnlocked) = await _userVerificationMediatorService.PerformOSUnlockAsync();
            if (canPerformOSUnlock)
            {
                return isOSUnlocked;
            }

            var (canPerformUnlockWithPin, pinVerified) = await _userVerificationMediatorService.VerifyPinCodeAsync();
            if (canPerformUnlockWithPin)
            {
                return pinVerified;
            }

            var (canPerformUnlockWithMasterPassword, mpVerified) = await _userVerificationMediatorService.VerifyMasterPasswordAsync(false);
            if (canPerformUnlockWithMasterPassword)
            {
                return mpVerified;
            }

            // TODO: Setup PIN code. For the sake of simplicity, we're not implementing this step now and just telling the user to do it in the main app.

            await _platformUtilsService.ShowDialogAsync(AppResources.VerificationRequiredForThisActionSetUpAnUnlockMethodInBitwardenToContinue,
                string.Format(AppResources.VerificationRequiredByX, options.RpId),
                AppResources.Ok);

            return false;
        }
    }
}
