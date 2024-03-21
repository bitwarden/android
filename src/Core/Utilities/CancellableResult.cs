namespace Bit.Core.Utilities
{
    public readonly struct CancellableResult<T>
    {
        public CancellableResult(T result, bool isCancelled = false)
        {
            Result = result;
            IsCancelled = isCancelled;
        }

        public T Result { get; }

        public bool IsCancelled { get; }
    }
}
