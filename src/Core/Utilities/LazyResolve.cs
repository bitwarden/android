using System;

namespace Bit.Core.Utilities
{
    public class LazyResolve<T> : Lazy<T>
    {
        public LazyResolve(string containerKey)
            : base(() => ServiceContainer.Resolve<T>(containerKey))
        {
        }
    }
}
