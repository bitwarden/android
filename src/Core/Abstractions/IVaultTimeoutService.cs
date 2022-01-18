using System;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IVaultTimeoutService
    {
        Task CheckVaultTimeoutAsync();
        Task ClearAsync(string userId = null);
        Task<bool> IsLockedAsync(string userId = null);
        Task<Tuple<bool, bool>> IsPinLockSetAsync();
        Task<bool> IsBiometricLockSetAsync();
        Task LockAsync(bool allowSoftLock = false, bool userInitiated = false, string userId = null);
        Task LogOutAsync(bool userInitiated = true, string userId = null);
        Task SetVaultTimeoutOptionsAsync(int? timeout, string action);
        Task<int?> GetVaultTimeout(string userId = null);
    }
}
