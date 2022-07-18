using System;
namespace Bit.iOS.Core.Renderers.CollectionView
{
    public class CollectionException : Exception
    {
        public CollectionException(string message)
            : base(message)
        {
        }

        public CollectionException(string message, Exception innerEx)
            : base(message, innerEx)
        {
        }
    }
}
