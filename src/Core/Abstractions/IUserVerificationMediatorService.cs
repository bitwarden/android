using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Abstractions
{
    public interface IUserVerificationMediatorService
    {
        Task<CancellableResult<bool>> VerifyUserForFido2Async(Fido2UserVerificationOptions options);
        Task<bool> CanPerformUserVerificationPreferredAsync(Fido2UserVerificationOptions options);
        Task<bool> ShouldPerformMasterPasswordRepromptAsync(Fido2UserVerificationOptions options);
        Task<bool> ShouldEnforceFido2RequiredUserVerificationAsync(Fido2UserVerificationOptions options);
        Task<CancellableResult<UVResult>> PerformOSUnlockAsync();
        Task<CancellableResult<UVResult>> VerifyPinCodeAsync();
        Task<CancellableResult<UVResult>> VerifyMasterPasswordAsync(bool isMasterPasswordReprompt);

        public struct UVResult
        {
            public UVResult(bool canPerform, bool isVerified)
            {
                CanPerform = canPerform;
                IsVerified = isVerified;
            }

            public bool CanPerform { get; set; }
            public bool IsVerified { get; set; }
        }
    }
}
