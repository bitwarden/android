using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Services.UserVerification
{
    public class Fido2UserVerificationPreferredServiceStrategy : IUserVerificationServiceStrategy
    {
        private readonly IUserVerificationMediatorService _userVerificationMediatorService;

        public Fido2UserVerificationPreferredServiceStrategy(IUserVerificationMediatorService userVerificationMediatorService)
        {
            _userVerificationMediatorService = userVerificationMediatorService;
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

            return new CancellableResult<bool>(false);
        }
    }
}
