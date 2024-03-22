using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;
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

        public async Task<CancellableResult<bool>> VerifyUserForFido2Async(Fido2UserVerificationOptions options)
        {
            if (options.HasVaultBeenUnlockedInTransaction)
            {
                return new CancellableResult<bool>(true);
            }

            if (options.OnNeedUITask != null)
            {
                await options.OnNeedUITask();
            }

            var osUnlockVerification = await _userVerificationMediatorService.PerformOSUnlockAsync();
            if (osUnlockVerification.IsCancelled)
            {
                return new CancellableResult<bool>(false, true);
            }
            if (osUnlockVerification.Result.CanPerform)
            {
                return new CancellableResult<bool>(osUnlockVerification.Result.IsVerified);
            }

            var pinVerification = await _userVerificationMediatorService.VerifyPinCodeAsync();
            if (pinVerification.IsCancelled)
            {
                return new CancellableResult<bool>(false, true);
            }
            if (pinVerification.Result.CanPerform)
            {
                return new CancellableResult<bool>(pinVerification.Result.IsVerified);
            }

            var mpVerification = await _userVerificationMediatorService.VerifyMasterPasswordAsync(false);
            if (mpVerification.IsCancelled)
            {
                return new CancellableResult<bool>(false, true);
            }
            if (mpVerification.Result.CanPerform)
            {
                return new CancellableResult<bool>(mpVerification.Result.IsVerified);
            }

            // TODO: Setup PIN code. For the sake of simplicity, we're not implementing this step now and just telling the user to do it in the main app.

            await _platformUtilsService.ShowDialogAsync(AppResources.VerificationRequiredForThisActionSetUpAnUnlockMethodInBitwardenToContinue,
                string.Format(AppResources.VerificationRequiredByX, options.RpId),
                AppResources.Ok);

            return new CancellableResult<bool>(false);
        }
    }
}
