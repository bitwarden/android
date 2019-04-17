using System;
using System.Globalization;

namespace Bit.Core.Abstractions
{
    public interface II18nService
    {
        CultureInfo Culture { get; set; }
        StringComparer StringComparer { get; }
        string T(string id, string p1 = null, string p2 = null, string p3 = null);
        string Translate(string id, string p1 = null, string p2 = null, string p3 = null);
    }
}