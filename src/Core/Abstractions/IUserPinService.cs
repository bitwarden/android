using Bit.Core.Services;

namespace Bit.Core.Abstractions
{
    public interface IUserPinService
    {
        Task<bool> IsPinLockEnabledAsync();
        Task SetupPinAsync(string pin, bool requireMasterPasswordOnRestart);
        Task<bool> VerifyPinAsync(string inputPin);
        Task<bool> VerifyPinAsync(string inputPin, string email, KdfConfig kdfConfig, PinLockType pinLockType);
    }
}
