using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Abstractions
{
    public interface IUserVerificationMediatorService
    {
        Task<bool> VerifyUserForFido2Async(Fido2UserVerificationOptions options);
        Task<bool> CanPerformUserVerificationPreferredAsync(Fido2UserVerificationOptions options);
        Task<bool> ShouldPerformMasterPasswordRepromptAsync(Fido2UserVerificationOptions options);
        Task<bool> ShouldEnforceFido2RequiredUserVerificationAsync(Fido2UserVerificationOptions options);
        Task<(bool CanPerfom, bool IsUnlocked)> PerformOSUnlockAsync();
        Task<(bool canPerformUnlockWithPin, bool pinVerified)> VerifyPinCodeAsync();
        Task<(bool canPerformUnlockWithMasterPassword, bool mpVerified)> VerifyMasterPasswordAsync(bool isMasterPasswordReprompt);
    }
}
