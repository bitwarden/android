using Bit.Core.Abstractions;
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

            return false;
        }
    }
}
