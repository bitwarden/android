using Bit.App.Enums;
using System;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface ILockService
    {
        void UpdateLastActivity();
        Task<LockType> GetLockTypeAsync(bool forceLock);
        Task CheckLockAsync(bool forceLock);
        bool TopPageIsLock();
        void StartLockTimer();
    }
}