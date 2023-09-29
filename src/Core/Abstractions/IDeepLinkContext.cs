using System;

namespace Bit.App.Abstractions
{
    public interface IDeepLinkContext
    {
        bool OnNewUri(Uri uri);
    }
}
