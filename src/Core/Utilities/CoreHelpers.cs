using System;

namespace Bit.Core.Utilities
{
    public static class CoreHelpers
    {
        public static readonly DateTime Epoc = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public static bool InDebugMode()
        {
#if DEBUG
            return true;
#else
            return false;
#endif
        }
    }
}
