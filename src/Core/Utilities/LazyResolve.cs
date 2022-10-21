using System;

namespace Bit.Core.Utilities
{
    public class LazyResolve<T> : Lazy<T> where T : class
    {
        public LazyResolve()
            : base(() => ServiceContainer.Resolve<T>())
        {
        }

        public LazyResolve(string containerKey)
            : base(() => ServiceContainer.Resolve<T>(containerKey))
        {
        }
    }
}
