using System.Globalization;

namespace Bit.App.Abstractions
{
    public interface ILocalizeService
    {
        CultureInfo GetCurrentCultureInfo();
    }
}
