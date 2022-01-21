using System;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IVaultTimeoutService
    {
        long? DelayLockAndLogoutMs { get; set; }

        Task CheckVaultTimeoutAsync();
        Task<bool> ShouldTimeoutAsync(string userId = null);
        Task ExecuteTimeoutActionAsync(string userId = null);
        Task ClearAsync(string userId = null);
        Task<bool> IsLockedAsync(string userId = null);
        Task<Tuple<bool, bool>> IsPinLockSetAsync(string userId = null);
        Task<bool> IsBiometricLockSetAsync(string userId = null);
        Task LockAsync(bool allowSoftLock = false, bool userInitiated = false, string userId = null);
        Task LogOutAsync(bool userInitiated = true, string userId = null);
        Task SetVaultTimeoutOptionsAsync(int? timeout, string action);
        Task<int?> GetVaultTimeout(string userId = null);
    }
}
