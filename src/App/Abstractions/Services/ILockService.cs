using Bit.App.Enums;
using System;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface ILockService
    {
        void UpdateLastActivity(DateTime? activityDate = null);
        Task<LockType> GetLockTypeAsync(bool forceLock);
    }
}