using System;

namespace Bit.App.Abstractions
{
    public interface IDeepLinkContext
    {
        public Uri CurrentUri { get; }

        bool HandleUri(Uri uri);
        void Clear();
    }
}
