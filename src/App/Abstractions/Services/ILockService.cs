using Bit.App.Enums;
using System;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface ILockService
    {
        void UpdateLastActivity();
        Task<LockType> GetLockTypeAsync(bool forceLock, bool onlyIfAlreadyLocked = false);
        Task CheckLockAsync(bool forceLock, bool onlyIfAlreadyLocked = false);
        bool TopPageIsLock();
    }
}