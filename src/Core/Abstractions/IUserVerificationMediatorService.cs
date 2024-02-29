using Bit.Core.Services;

namespace Bit.Core.Abstractions
{
    public interface IUserVerificationMediatorService
    {
        Task<bool> VerifyUserForFido2Async(Fido2VerificationOptions options);
        Task<(bool CanPerfom, bool IsUnlocked)> PerformOSUnlockAsync();
        Task<(bool canPerformUnlockWithPin, bool pinVerified)> VerifyPinCodeAsync();
        Task<(bool canPerformUnlockWithMasterPassword, bool mpVerified)> VerifyMasterPasswordAsync();
    }
}
