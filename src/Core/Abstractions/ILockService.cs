using System;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface ILockService
    {
        CipherString PinProtectedKey { get; set; }
        bool FingerprintLocked { get; set; }

        Task CheckLockAsync();
        Task ClearAsync();
        Task<bool> IsLockedAsync();
        Task<Tuple<bool, bool>> IsPinLockSetAsync();
        Task<bool> IsFingerprintLockSetAsync();
        Task LockAsync(bool allowSoftLock = false, bool userInitiated = false);
        Task SetLockOptionAsync(int? lockOption);
    }
}
