using System;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public enum AwaiterPrecondition
    {
        EnvironmentUrlsInited
    }

    public interface IConditionedAwaiterManager
    {
        Task GetAwaiterForPrecondition(AwaiterPrecondition awaiterPrecondition);
        void SetAsCompleted(AwaiterPrecondition awaiterPrecondition);
        void SetException(AwaiterPrecondition awaiterPrecondition, Exception ex);
    }
}
