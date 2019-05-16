using System;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface ILockService
    {
        bool PinLocked { get; set; }
        bool FingerprintLocked { get; set; }

        Task CheckLockAsync();
        Task ClearAsync();
        Task<bool> IsLockedAsync();
        Task<Tuple<bool, bool>> IsPinLockSetAsync();
        Task<bool> IsFingerprintLockSetAsync();
        Task LockAsync(bool allowSoftLock = false);
        Task SetLockOptionAsync(int? lockOption);
    }
}