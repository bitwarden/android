using System.Collections.Concurrent;
using Bit.Core.Abstractions;

namespace Bit.Core.Services
{
    public class ConditionedAwaiterManager : IConditionedAwaiterManager
    {
        private readonly ConcurrentDictionary<AwaiterPrecondition, TaskCompletionSource<bool>> _preconditionsTasks = new ConcurrentDictionary<AwaiterPrecondition, TaskCompletionSource<bool>>
        {
            [AwaiterPrecondition.EnvironmentUrlsInited] = new TaskCompletionSource<bool>(),
            [AwaiterPrecondition.AndroidWindowCreated] = new TaskCompletionSource<bool>(),
            [AwaiterPrecondition.AutofillIOSExtensionViewDidAppear] = new TaskCompletionSource<bool>()
        };

        public Task GetAwaiterForPrecondition(AwaiterPrecondition awaiterPrecondition)
        {
            if (_preconditionsTasks.TryGetValue(awaiterPrecondition, out var tcs))
            {
                return tcs.Task;
            }

            return Task.CompletedTask;
        }

        public void SetAsCompleted(AwaiterPrecondition awaiterPrecondition)
        {
            if (_preconditionsTasks.TryGetValue(awaiterPrecondition, out var tcs))
            {
                tcs.TrySetResult(true);
            }
        }

        public void SetException(AwaiterPrecondition awaiterPrecondition, Exception ex)
        {
            if (_preconditionsTasks.TryGetValue(awaiterPrecondition, out var tcs))
            {
                tcs.TrySetException(ex);
            }
        }

        public void Recreate(AwaiterPrecondition awaiterPrecondition)
        {
            if (_preconditionsTasks.TryRemove(awaiterPrecondition, out var oldTcs))
            {
                oldTcs.TrySetCanceled();

                _preconditionsTasks.TryAdd(awaiterPrecondition, new TaskCompletionSource<bool>());
            }
        }
    }
}
