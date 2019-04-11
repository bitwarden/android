using System.Globalization;

namespace Bit.Core.Abstractions
{
    public interface II18nService
    {
        CultureInfo Culture { get; set; }
        string T(string id, params string[] p);
        string Translate(string id, params string[] p);
    }
}