namespace Bit.Core.Abstractions
{
    public enum AwaiterPrecondition
    {
        EnvironmentUrlsInited,
        AndroidWindowCreated,
        AutofillIOSExtensionViewDidAppear
    }

    public interface IConditionedAwaiterManager
    {
        Task GetAwaiterForPrecondition(AwaiterPrecondition awaiterPrecondition);
        void SetAsCompleted(AwaiterPrecondition awaiterPrecondition);
        void SetException(AwaiterPrecondition awaiterPrecondition, Exception ex);
        void Recreate(AwaiterPrecondition awaiterPrecondition);
    }
}
