using Bit.App.Enums;
using System;

namespace Bit.App.Abstractions
{
    public interface ILockService
    {
        void UpdateLastActivity(DateTime? activityDate = null);
        LockType GetLockType(bool forceLock);
    }
}