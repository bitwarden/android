using Bit.App.Enums;

namespace Bit.App.Abstractions
{
    public interface ILockService
    {
        LockType GetLockType(bool forceLock);
    }
}