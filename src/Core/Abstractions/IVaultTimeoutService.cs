using System;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IVaultTimeoutService
    {
        EncString PinProtectedKey { get; set; }
        bool BiometricLocked { get; set; }

        Task CheckVaultTimeoutAsync();
        Task ClearAsync();
        Task<bool> IsLockedAsync();
        Task<Tuple<bool, bool>> IsPinLockSetAsync();
        Task<bool> IsBiometricLockSetAsync();
        Task LockAsync(bool allowSoftLock = false, bool userInitiated = false);
        Task LogOutAsync();
        Task SetVaultTimeoutOptionsAsync(int? timeout, string action);
    }
}
